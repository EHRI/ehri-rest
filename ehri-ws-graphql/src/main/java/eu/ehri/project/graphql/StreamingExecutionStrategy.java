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
import graphql.UnresolvedTypeError;
import graphql.execution.*;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.language.Field;
import graphql.schema.*;
import graphql.util.FpKit;
import graphql.util.LogKit;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

import static graphql.execution.FieldCollectorParameters.newParameters;
import static graphql.schema.GraphQLTypeUtil.*;


/**
 * Streaming version of an execution strategy.
 *
 * TODO: this class duplicates with slight modifications a lot of logic
 * from the {@link ExecutionStrategy} class - find a way to clean it up
 * and rationalise things for easier maintenance and upgrading.
 */
public class StreamingExecutionStrategy extends ExecutionStrategy {
    private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(ExecutionStrategy.class);

    private final JsonGenerator generator;

    public StreamingExecutionStrategy(JsonGenerator generator) {
        super();
        this.generator = generator;
    }

    public void execute(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws IOException {
        InstrumentationExecutionStrategyParameters instrumentationParameters = new InstrumentationExecutionStrategyParameters(executionContext, parameters);

        ExecutionStrategyInstrumentationContext executionStrategyCtx = executionContext.getInstrumentation().beginExecutionStrategy(instrumentationParameters);

        MergedSelectionSet fields = parameters.getFields();

        generator.writeStartObject();
        for (String fieldName : fields.keySet()) {
            MergedField currentField = fields.getSubField(fieldName);
            ResultPath fieldPath = parameters.getPath().segment(mkNameForPath(currentField));
            ExecutionStrategyParameters newParameters = parameters
                    .transform(builder -> builder.field(currentField).path(fieldPath).parent(parameters));
            generator.writeFieldName(fieldName);

            final CompletableFuture<FetchedValue> completableFuture = fetchField(executionContext, newParameters);
            FetchedValue fetchedValue = completableFuture.join();
            completeField(generator, executionContext, newParameters, fetchedValue);
        }
        generator.writeEndObject();

        executionStrategyCtx.onCompleted(null, null);
    }

    private void completeField(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) throws IOException {
        Field field = parameters.getField().getSingleField();
        GraphQLObjectType parentType = (GraphQLObjectType) parameters.getExecutionStepInfo().getUnwrappedNonNullType();
        GraphQLFieldDefinition fieldDef = getFieldDef(executionContext.getGraphQLSchema(), parentType, field);
        ExecutionStepInfo executionStepInfo = createExecutionStepInfo(executionContext, parameters, fieldDef, parentType);

        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, executionStepInfo);

        ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                builder.executionStepInfo(executionStepInfo)
                        .source(fetchedValue.getFetchedValue())
                        .localContext(fetchedValue.getLocalContext())
                        .nonNullFieldValidator(nonNullableFieldValidator)
        );

        completeValue(generator, executionContext, newParameters);
    }


    private void completeValue(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters) throws IOException {
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        final ValueUnboxer valueUnboxer = executionContext.getValueUnboxer();
        GraphQLType fieldType = executionStepInfo.getUnwrappedNonNullType();
        Object result = valueUnboxer.unbox(parameters.getSource());

        if (result == null) {
            generator.writeNull();
        } else if (isList(fieldType)) {
            completeValueForList(generator, executionContext, parameters, result);
        } else if (isScalar(fieldType)) {
            completeValueForScalar(generator, executionContext, parameters, (GraphQLScalarType) fieldType, result);
        } else if (isEnum(fieldType)) {
            completeValueForEnum(generator, executionContext, parameters, (GraphQLEnumType) fieldType, result);
        } else {
            // when we are here, we have a complex type: Interface, Union or Object
            // and we must go deeper
            //
            GraphQLObjectType resolvedObjectType;
            try {
                resolvedObjectType = resolveType(executionContext, parameters, fieldType);
                completeValueForObject(generator, executionContext, parameters, resolvedObjectType, result);
            } catch (UnresolvedTypeException ex) {
                // consider the result to be null and add the error on the context
                handleUnresolvedTypeProblem(executionContext, parameters, ex);
                // and validate the field is nullable, if non-nullable throw exception
                parameters.getNonNullFieldValidator().validate(parameters.getPath(), null);
            }
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

    private void completeValueForEnum(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, GraphQLEnumType enumType, Object result) throws IOException {
        Object serialized;
        try {
            serialized = enumType.serialize(result);
        } catch (CoercingSerializeException e) {
            serialized = handleCoercionProblem(executionContext, parameters, e);
        }
        try {
            serialized = parameters.getNonNullFieldValidator().validate(parameters.getPath(), serialized);
        } catch (NonNullableFieldWasNullException e) {
            e.printStackTrace();
            // FIXME ?
        }
        generator.writeObject(serialized);
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
            e.printStackTrace();
        }
        if (resultIterable == null) {
            // FIXME
            throw new RuntimeException("Iterable result is null!");
        }
    }

    private void completeValueForList(JsonGenerator generator, ExecutionContext executionContext, ExecutionStrategyParameters parameters, Iterable<Object> result) throws IOException {
        OptionalInt size = FpKit.toSize(result);
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        generator.writeStartArray();
        int index = 0;
        for (Object item : result) {
            index++;
            ResultPath indexedPath = parameters.getPath().segment(index);

            ExecutionStepInfo stepInfoForListElement = executionStepInfoFactory.newExecutionStepInfoForListElement(executionStepInfo, index);

            NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, stepInfoForListElement);

            int finalIndex = index;
            FetchedValue value = unboxPossibleDataFetcherResult(executionContext, parameters, item);

            ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                    builder.executionStepInfo(stepInfoForListElement)
                            .nonNullFieldValidator(nonNullableFieldValidator)
                            .listSize(size.orElse(-1)) // -1 signals that we don't know the size
                            .localContext(value.getLocalContext())
                            .currentListIndex(finalIndex)
                            .path(indexedPath)
                            .source(value.getFetchedValue())
            );
            completeValue(generator, executionContext, newParameters);
            index++;
        }
        generator.writeEndArray();
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext executionContext, ExecutionStrategyParameters parameters) {
        try {
            execute(generator, executionContext, parameters);
            return Async.toCompletableFuture(new ExecutionResultImpl(null, null));
        } catch (IOException e) {
            return Async.exceptionallyCompletedFuture(e);
        }
    }

    private void handleUnresolvedTypeProblem(ExecutionContext context, ExecutionStrategyParameters parameters, UnresolvedTypeException e) {
        UnresolvedTypeError error = new UnresolvedTypeError(parameters.getPath(), parameters.getExecutionStepInfo(), e);
        logNotSafe.warn(error.getMessage(), e);
        context.addError(error);

    }

    private Object handleCoercionProblem(ExecutionContext context, ExecutionStrategyParameters parameters, CoercingSerializeException e) {
        SerializationError error = new SerializationError(parameters.getPath(), e);
        context.addError(error);

        return null;
    }
}
