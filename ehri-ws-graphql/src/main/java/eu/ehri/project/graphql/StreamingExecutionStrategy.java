/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import graphql.ExecutionResultImpl;
import graphql.SerializationError;
import graphql.TrivialDataFetcher;
import graphql.execution.*;
//import graphql.execution.ExecutionTypeInfo;
import graphql.execution.directives.QueryDirectives;
import graphql.execution.directives.QueryDirectivesImpl;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.language.Field;
import graphql.normalized.NormalizedField;
import graphql.schema.*;
import graphql.util.FpKit;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

//import static graphql.execution.ExecutionTypeInfo.newTypeInfo;
import static graphql.execution.Async.exceptionallyCompletedFuture;
import static graphql.execution.FieldCollectorParameters.newParameters;
import static graphql.execution.FieldValueInfo.CompleteValueType.*;
import static graphql.execution.FieldValueInfo.CompleteValueType.OBJECT;
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static graphql.schema.GraphQLTypeUtil.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
//import static graphql.schema.DataFetchingEnvironmentBuilder.newDataFetchingEnvironment;

/**
 * Streaming version of an execution strategy.
 *
 * TODO: this class duplicates with slight modifications a lot of logic
 * from the {@link ExecutionStrategy} class - find a way to clean it up
 * and rationalise things for easier maintenance and upgrading.
 */
public class StreamingExecutionStrategy extends ExecutionStrategy {

    public void execute(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws IOException {
        MergedSelectionSet fields = parameters.getFields();
        Set<String> fieldNames = fields.keySet();
        List<CompletableFuture<FieldValueInfo>> futures = new ArrayList<>(fieldNames.size());
        List<String> resolvedFields = new ArrayList<>(fieldNames.size());

        generator.writeStartObject();
        for (String fieldName : parameters.getFields().keySet()) {
            MergedField currentField = fields.getSubField(fieldName);
            ResultPath fieldPath = parameters.getPath().segment(mkNameForPath(currentField));
            ExecutionStrategyParameters newParameters = parameters
                    .transform(builder -> builder.field(currentField).path(fieldPath).parent(parameters));
            System.out.println("Writing field: " + fieldName + " subfield " + currentField.getName() + " path " + fieldPath);
            generator.writeFieldName(fieldName);
            final CompletableFuture<FetchedValue> completableFuture = fetchField(executionContext, newParameters);
            FetchedValue fetchedValue = completableFuture.join();
            System.out.println("Completing field with value " + fetchedValue.getRawFetchedValue());
            completeField(generator, executionContext, newParameters, fetchedValue);
        }
        generator.writeEndObject();
    }

//    protected void fetchField(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
//        MergedField field = parameters.getField();
//        System.out.println("Fetch field: " + field);
//        GraphQLObjectType parentType = (GraphQLObjectType) parameters.getExecutionStepInfo().getUnwrappedNonNullType();
//        GraphQLFieldDefinition fieldDef = getFieldDef(executionContext.getGraphQLSchema(), parentType, field.getSingleField());
//
//        GraphQLCodeRegistry codeRegistry = executionContext.getGraphQLSchema().getCodeRegistry();
//        GraphQLOutputType fieldType = fieldDef.getType();
//
//        // if the DF (like PropertyDataFetcher) does not use the arguments of execution step info then dont build any
//        ExecutionStepInfo executionStepInfo = createExecutionStepInfo(executionContext, parameters, fieldDef, parentType);
//        Map<String, Object> argumentValues = executionStepInfo.getArguments();
//
//        Supplier<NormalizedField> normalizedFieldSupplier = getNormalizedField(executionContext, parameters, () -> executionStepInfo);
//
//        // DataFetchingFieldSelectionSet and QueryDirectives is a supplier of sorts - eg a lazy pattern
//        DataFetchingFieldSelectionSet fieldCollector = DataFetchingFieldSelectionSetImpl.newCollector(fieldType, normalizedFieldSupplier);
//        QueryDirectives queryDirectives = new QueryDirectivesImpl(field, executionContext.getGraphQLSchema(), executionContext.getVariables());
//
//
//        DataFetchingEnvironment environment = newDataFetchingEnvironment(executionContext)
//                .source(parameters.getSource())
//                .localContext(parameters.getLocalContext())
//                .arguments(argumentValues)
//                .fieldDefinition(fieldDef)
//                .mergedField(parameters.getField())
//                .fieldType(fieldType)
//                .executionStepInfo(executionStepInfo)
//                .parentType(parentType)
//                .selectionSet(fieldCollector)
//                .queryDirectives(queryDirectives)
//                .build();
//
//        DataFetcher<?> dataFetcher = codeRegistry.getDataFetcher(parentType, fieldDef);
//        Object fetchedValue = null;
//        try {
//            fetchedValue = dataFetcher.get(environment);
//            completeField(generator, executionContext, parameters, FetchedValue.newFetchedValue().fetchedValue(fetchedValue).build());
//        } catch (Exception e) {
//            // TODO
//            System.out.println("Fetch field exception: ");
//            e.printStackTrace();
//        }
//
//
//    }


    private void completeField(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) throws IOException {
        Field field = parameters.getField().getSingleField();
        GraphQLObjectType parentType = (GraphQLObjectType) parameters.getExecutionStepInfo().getUnwrappedNonNullType();
        GraphQLFieldDefinition fieldDef = getFieldDef(executionContext.getGraphQLSchema(), parentType, field);
        ExecutionStepInfo executionStepInfo = createExecutionStepInfo(executionContext, parameters, fieldDef, parentType);

//        Instrumentation instrumentation = executionContext.getInstrumentation();
//        InstrumentationFieldCompleteParameters instrumentationParams = new InstrumentationFieldCompleteParameters(executionContext, parameters, () -> executionStepInfo, fetchedValue);
//        InstrumentationContext<ExecutionResult> ctxCompleteField = instrumentation.beginFieldComplete(
//                instrumentationParams
//        );

        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, executionStepInfo);

        ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                builder.executionStepInfo(executionStepInfo)
                        .source(fetchedValue.getFetchedValue())
                        .localContext(fetchedValue.getLocalContext())
                        .nonNullFieldValidator(nonNullableFieldValidator)
        );

        System.out.println("Calling complete field with fetched: " + fetchedValue);
        completeValue(generator, executionContext, newParameters);

//        ctxCompleteField.onCompleted(newParameters.getSource(), null);
    }


    private void completeValue(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws IOException {
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        final Object source = parameters.getSource();
        final ValueUnboxer valueUnboxer = executionContext.getValueUnboxer();
        Object result = valueUnboxer.unbox(source);
        GraphQLType fieldType = executionStepInfo.getUnwrappedNonNullType();
        System.out.println("Fieldtype: " + fieldType + " Source: " + source);

        if (result == null) {
            System.out.println("Writing null " + source);
            generator.writeNull();
        } else if (isList(fieldType)) {
            System.out.println("Writing list " + result);
            completeValueForList(generator, executionContext, parameters, result);
        } else if (isScalar(fieldType)) {
            System.out.println("Writing scalar " + result);
            completeValueForScalar(generator, executionContext, parameters, (GraphQLScalarType) fieldType, result);
        } else if (isEnum(fieldType)) {
            System.out.println("Writing enum " + result);
            completeValueForEnum(generator, (GraphQLEnumType) fieldType, result);
        } else {
            // when we are here, we have a complex type: Interface, Union or Object
            // and we must go deeper
            //
            GraphQLObjectType resolvedObjectType;
            try {
                resolvedObjectType = resolveType(executionContext, parameters, fieldType);
//            fieldValue = completeValueForObject(executionContext, parameters, resolvedObjectType, result);
                System.out.println("Writing object... " + result);
                completeValueForObject(generator, executionContext, parameters, resolvedObjectType, result);

            } catch (UnresolvedTypeException ex) {
                System.out.println("Unresolved type: " + ex);
                ex.printStackTrace();
                // consider the result to be null and add the error on the context
//            handleUnresolvedTypeProblem(executionContext, parameters, ex);
                // and validate the field is nullable, if non-nullable throw exception
                parameters.getNonNullFieldValidator().validate(parameters.getPath(), null);
                // complete the field as null
//            fieldValue = completedFuture(new ExecutionResultImpl(null, null));
            }
//        return FieldValueInfo.newFieldValueInfo(OBJECT).fieldValue(fieldValue).build();


        }
    }

    protected void completeValueForObject(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, GraphQLObjectType resolvedObjectType, Object result) throws IOException {
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        FieldCollectorParameters collectorParameters = newParameters()
                .schema(executionContext.getGraphQLSchema())
                .objectType(resolvedObjectType)
                .fragments(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .build();

        final MergedField field = parameters.getField();
        System.out.println("Merged field: " + field);
        MergedSelectionSet subFields = fieldCollector.collectFields(collectorParameters, field);

        ExecutionStepInfo newExecutionStepInfo = executionStepInfo.changeTypeWithPreservedNonNull(resolvedObjectType);
        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, newExecutionStepInfo);

        ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                builder.executionStepInfo(newExecutionStepInfo)
                        .fields(subFields)
                        .nonNullFieldValidator(nonNullableFieldValidator)
                        .source(result)
        );

        // Calling this from the executionContext to ensure we shift back from mutation strategy to the query strategy.

        execute(generator, executionContext, newParameters);
    }

    private void completeValueForEnum(JsonGenerator generator, GraphQLEnumType enumType, Object result) throws IOException {
        System.out.println();
    }

    private void completeValueForScalar(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, GraphQLScalarType scalarType, Object result) throws IOException {
        Object serialized;
        try {
            serialized = scalarType.getCoercing().serialize(result);
        } catch (CoercingSerializeException e) {
            serialized = handleCoercionProblem(executionContext, parameters, e);
        }

        // TODO: fix that: this should not be handled here
        //6.6.1 http://facebook.github.io/graphql/#sec-Field-entries
        if (serialized instanceof Double && ((Double) serialized).isNaN()) {
            serialized = null;
        }
        try {
            serialized = parameters.getNonNullFieldValidator().validate(parameters.getPath(), serialized);
        } catch (NonNullableFieldWasNullException e) {
            e.printStackTrace();
            // FIXME: ?
        }
        generator.writeObject(serialized);
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object result) throws IOException {
        Iterable<Object> resultIterable = toIterable(executionContext, parameters, result);
        try {
            resultIterable = parameters.getNonNullFieldValidator().validate(parameters.getPath(), resultIterable);
            completeValueForList(generator, executionContext, parameters, resultIterable);
        } catch (NonNullableFieldWasNullException e) {
            // FIXME
        }
        if (resultIterable == null) {
            // FIXME
        }
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, Iterable<Object> result) throws IOException {
        OptionalInt size = FpKit.toSize(result);
        ExecutionStepInfo typeInfo = parameters.getExecutionStepInfo();

        generator.writeStartArray();
        int index = 0;
        for (Object item : result) {
            int finalIndex = index;
            FetchedValue value = unboxPossibleDataFetcherResult(executionContext, parameters, item);
            System.out.println("List index: " + index + " Item: " + item + " unboxed: " + value);
            ResultPath indexedPath = parameters.getPath().segment(index);
            ExecutionStepInfo stepInfoForListElement = executionStepInfoFactory.newExecutionStepInfoForListElement(typeInfo, index);
            NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, stepInfoForListElement);
            ExecutionStrategyParameters newParameters = parameters.transform(builder -> builder
                    .localContext(value.getLocalContext())
                    .listSize(size.orElse(-1))
                    .nonNullFieldValidator(nonNullableFieldValidator)
                    .executionStepInfo(stepInfoForListElement)
                    .currentListIndex(finalIndex)
                    .path(indexedPath)
                    .fields(parameters.getFields())
                    .source(value)
                    .build());

            completeValue(generator, executionContext, newParameters);
            index++;
        }
        generator.writeEndArray();
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        return new AsyncExecutionStrategy().execute(executionContext, parameters);
    }

    private void handleFetchingException(ExecutionContext executionContext,
                                         ExecutionStrategyParameters parameters,
                                         Field field,
                                         GraphQLFieldDefinition fieldDef,
                                         Map<String, Object> argumentValues,
                                         DataFetchingEnvironment environment,
                                         Throwable e) {
        // TODO
    }

    private Object handleCoercionProblem(ExecutionContext context, ExecutionStrategyParameters parameters, CoercingSerializeException e) {
        SerializationError error = new SerializationError(parameters.getPath(), e);
        System.out.println("Coercion problem: " + error.getMessage());
        context.addError(error);

        return null;
    }
}
