/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.graphql;

import com.fasterxml.jackson.core.JsonGenerator;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategy;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.SimpleExecutionStrategy;
import graphql.execution.TypeInfo;
import graphql.execution.TypeResolutionParameters;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.DataFetchingFieldSelectionSetImpl;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static graphql.execution.FieldCollectorParameters.newParameters;
import static graphql.execution.TypeInfo.newTypeInfo;
import static graphql.schema.DataFetchingEnvironmentBuilder.newDataFetchingEnvironment;

/**
 * Streaming version of an execution strategy.
 */
public class StreamingExecutionStrategy extends ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(StreamingExecutionStrategy.class);

    public void execute(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws IOException {
        generator.writeStartObject();
        for (String fieldName : parameters.fields().keySet()) {
            generator.writeFieldName(fieldName);
            resolveField(generator, executionContext, parameters, parameters.fields().get(fieldName));
        }
        generator.writeEndObject();
    }

    private void resolveField(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, List<Field> fields) throws IOException {
        GraphQLObjectType parentType = parameters.typeInfo().castType(GraphQLObjectType.class);
        GraphQLFieldDefinition fieldDef = getFieldDef(executionContext.getGraphQLSchema(), parentType, fields.get(0));

        Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDef.getArguments(), fields.get(0).getArguments(), executionContext.getVariables());

        GraphQLOutputType fieldType = fieldDef.getType();
        DataFetchingFieldSelectionSet fieldCollector = DataFetchingFieldSelectionSetImpl.newCollector(executionContext, fieldType, fields);

        DataFetchingEnvironment environment = newDataFetchingEnvironment(executionContext)
                .source(parameters.source())
                .arguments(argumentValues)
                .fieldDefinition(fieldDef)
                .fields(fields)
                .fieldType(fieldType)
                .parentType(parentType)
                .selectionSet(fieldCollector)
                .build();

        Instrumentation instrumentation = executionContext.getInstrumentation();

        InstrumentationContext<ExecutionResult> fieldCtx = instrumentation.beginField(new InstrumentationFieldParameters(executionContext, fieldDef, environment));

        InstrumentationContext<Object> fetchCtx = instrumentation.beginFieldFetch(new InstrumentationFieldFetchParameters(executionContext, fieldDef, environment));
        Object resolvedValue = null;
        try {
            resolvedValue = fieldDef.getDataFetcher().get(environment);

            fetchCtx.onEnd(resolvedValue);
        } catch (Exception e) {
            log.warn("Exception while fetching data", e);
            handleDataFetchingException(executionContext, fieldDef, argumentValues, parameters.path(), e);
            fetchCtx.onEnd(e);
        }

        TypeInfo fieldTypeInfo = newTypeInfo()
                .type(fieldType)
                .parentInfo(parameters.typeInfo())
                .build();


        ExecutionStrategyParameters newParameters = ExecutionStrategyParameters.newParameters()
                .typeInfo(fieldTypeInfo)
                .fields(parameters.fields())
                .arguments(argumentValues)
                .source(resolvedValue).build();

        completeValue(generator, executionContext, newParameters, fields);

        fieldCtx.onEnd((ExecutionResult) null);
    }

    private void completeValue(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, List<Field> fields) throws IOException {

        TypeInfo typeInfo = parameters.typeInfo();
        Object result = parameters.source();
        GraphQLType fieldType = parameters.typeInfo().type();

        if (result == null) {
            if (typeInfo.typeIsNonNull()) {
                // see http://facebook.github.io/graphql/#sec-Errors-and-Non-Nullability
                NonNullableFieldWasNullException nonNullException = new NonNullableFieldWasNullException(typeInfo, parameters.path());
                executionContext.addError(nonNullException);
                throw nonNullException;
            }
            generator.writeNull();
        } else if (fieldType instanceof GraphQLList) {
            completeValueForList(generator, executionContext, parameters, fields, result);
        } else if (fieldType instanceof GraphQLScalarType) {
            completeValueForScalar(generator, (GraphQLScalarType) fieldType, result);
        } else if (fieldType instanceof GraphQLEnumType) {
            completeValueForEnum(generator, (GraphQLEnumType) fieldType, result);
        } else {

            GraphQLObjectType resolvedType;
            if (fieldType instanceof GraphQLInterfaceType) {
                TypeResolutionParameters resolutionParams = TypeResolutionParameters.newParameters()
                        .graphQLInterfaceType((GraphQLInterfaceType) fieldType)
                        .field(fields.get(0))
                        .value(parameters.source())
                        .argumentValues(parameters.arguments())
                        .schema(executionContext.getGraphQLSchema()).build();
                resolvedType = resolveTypeForInterface(resolutionParams);

            } else if (fieldType instanceof GraphQLUnionType) {
                TypeResolutionParameters resolutionParams = TypeResolutionParameters.newParameters()
                        .graphQLUnionType((GraphQLUnionType) fieldType)
                        .field(fields.get(0))
                        .value(parameters.source())
                        .argumentValues(parameters.arguments())
                        .schema(executionContext.getGraphQLSchema()).build();
                resolvedType = resolveTypeForUnion(resolutionParams);
            } else {
                resolvedType = (GraphQLObjectType) fieldType;
            }

            FieldCollectorParameters collectorParameters = newParameters()
                    .schema(executionContext.getGraphQLSchema())
                    .objectType(resolvedType)
                    .fragments(executionContext.getFragmentsByName())
                    .variables(executionContext.getVariables())
                    .build();

            Map<String, List<Field>> subFields = fieldCollector.collectFields(collectorParameters, fields);

            ExecutionStrategyParameters newParameters = ExecutionStrategyParameters.newParameters()
                    .typeInfo(typeInfo.asType(resolvedType))
                    .fields(subFields)
                    .source(result).build();

            // Calling this from the executionContext to ensure we shift back from mutation strategy to the query strategy.
            execute(generator, executionContext, newParameters);
        }
    }

    private void completeValueForEnum(JsonGenerator generator, GraphQLEnumType enumType, Object result) throws IOException {
        generator.writeObject(enumType.getCoercing().serialize(result));
    }

    private void completeValueForScalar(JsonGenerator generator, GraphQLScalarType scalarType, Object result) throws IOException {
        Object serialized = scalarType.getCoercing().serialize(result);
        //6.6.1 http://facebook.github.io/graphql/#sec-Field-entries
        if (serialized instanceof Double && ((Double) serialized).isNaN()) {
            serialized = null;
        }
        generator.writeObject(serialized);
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, List<Field> fields, Object result) throws IOException {
        if (result.getClass().isArray()) {
            result = Arrays.asList((Object[]) result);
        }

        completeValueForList(generator, executionContext, parameters, fields, (Iterable<Object>) result);
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, List<Field> fields, Iterable<Object> result) throws IOException {
        TypeInfo typeInfo = parameters.typeInfo();
        GraphQLList fieldType = typeInfo.castType(GraphQLList.class);

        generator.writeStartArray();
        for (Object item : result) {
            ExecutionStrategyParameters newParameters = ExecutionStrategyParameters.newParameters()
                    .typeInfo(typeInfo.asType(fieldType.getWrappedType()))
                    .fields(parameters.fields())
                    .source(item).build();

            completeValue(generator, executionContext, newParameters, fields);
        }
        generator.writeEndArray();
    }


    @Override
    public ExecutionResult execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        return new SimpleExecutionStrategy().execute(executionContext, parameters);
    }
}
