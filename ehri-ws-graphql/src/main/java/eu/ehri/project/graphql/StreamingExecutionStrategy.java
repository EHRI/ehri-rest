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
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategy;
import graphql.execution.SimpleExecutionStrategy;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLUnionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streaming version of an execution strategy.
 */
public class StreamingExecutionStrategy extends ExecutionStrategy {

    public void execute(JsonGenerator generator, ExecutionContext executionContext, GraphQLObjectType parentType, Object source, Map<String, List<Field>> fields) throws IOException {

        generator.writeStartObject();
        for (String fieldName : fields.keySet()) {
            generator.writeFieldName(fieldName);
            List<Field> fieldList = fields.get(fieldName);
            resolveField(generator, executionContext, parentType, source, fieldList);
        }
        generator.writeEndObject();
    }

    private void resolveField(JsonGenerator generator, ExecutionContext executionContext, GraphQLObjectType parentType, Object source, List<Field> fields) throws IOException {
        GraphQLFieldDefinition fieldDef = getFieldDef(executionContext.getGraphQLSchema(), parentType, fields.get(0));

        Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDef.getArguments(), fields.get(0).getArguments(), executionContext.getVariables());
        DataFetchingEnvironment environment = new DataFetchingEnvironment(
                source,
                argumentValues,
                executionContext.getRoot(),
                fields,
                fieldDef.getType(),
                parentType,
                executionContext.getGraphQLSchema()
        );

        Object resolvedValue = null;
        try {
            resolvedValue = fieldDef.getDataFetcher().get(environment);
        } catch (Exception e) {
            executionContext.addError(new ExceptionWhileDataFetching(e));
        }

        completeValue(generator, executionContext, fieldDef.getType(), fields, resolvedValue);
    }

    private void completeValue(JsonGenerator generator, ExecutionContext executionContext, GraphQLOutputType fieldType, List<Field> fields, Object result) throws IOException {
        if (fieldType instanceof GraphQLNonNull) {
            GraphQLNonNull graphQLNonNull = (GraphQLNonNull) fieldType;
            ExecutionResult completed = completeValue(executionContext, graphQLNonNull.getWrappedType(), fields, result);
            if (completed == null) {
                throw new GraphQLException("Cannot return null for non-nullable type: " + fields);
            }
            generator.writeObject(result);

        } else if (result == null) {
            generator.writeNull();
        } else if (fieldType instanceof GraphQLList) {
            completeValueForList(generator, executionContext, (GraphQLList) fieldType, fields, result);
        } else if (fieldType instanceof GraphQLScalarType) {
            completeValueForScalar(generator, (GraphQLScalarType) fieldType, result);
        } else if (fieldType instanceof GraphQLEnumType) {
            completeValueForEnum(generator, (GraphQLEnumType) fieldType, result);
        } else {
            GraphQLObjectType resolvedType;
            if (fieldType instanceof GraphQLInterfaceType) {
                resolvedType = resolveType((GraphQLInterfaceType) fieldType, result);
            } else if (fieldType instanceof GraphQLUnionType) {
                resolvedType = resolveType((GraphQLUnionType) fieldType, result);
            } else {
                resolvedType = (GraphQLObjectType) fieldType;
            }

            Map<String, List<Field>> subFields = new LinkedHashMap<String, List<Field>>();
            List<String> visitedFragments = new ArrayList<String>();
            for (Field field : fields) {
                if (field.getSelectionSet() == null) continue;
                fieldCollector.collectFields(executionContext, resolvedType, field.getSelectionSet(), visitedFragments, subFields);
            }

            execute(generator, executionContext, resolvedType, result, subFields);
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

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, GraphQLList fieldType, List<Field> fields, Object result) throws IOException {
        if (result.getClass().isArray()) {
            result = Arrays.asList((Object[]) result);
        }

        completeValueForList(generator, executionContext, fieldType, fields, (Iterable<Object>) result);
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, GraphQLList fieldType, List<Field> fields, Iterable<Object> result) throws IOException {
        generator.writeStartArray();
        for (Object item : result) {
            completeValue(generator, executionContext, ((GraphQLOutputType) fieldType.getWrappedType()), fields, item);
        }
        generator.writeEndArray();
    }


    @Override
    public ExecutionResult execute(ExecutionContext executionContext, GraphQLObjectType graphQLObjectType, Object o, Map<String, List<Field>> fields) {
        return new SimpleExecutionStrategy().execute(executionContext, graphQLObjectType, o, fields);
    }
}
