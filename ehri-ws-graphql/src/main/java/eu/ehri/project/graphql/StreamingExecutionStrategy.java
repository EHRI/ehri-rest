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
import eu.ehri.extension.errors.ExecutionError;
import eu.ehri.project.persistence.Bundle;
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
 * TODO: find a way to integrate more instrumentation
 */
public class StreamingExecutionStrategy extends ExecutionStrategy {
    private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(ExecutionStrategy.class);

    private final JsonGenerator generator;

    public static StreamingExecutionStrategy jsonGenerator(JsonGenerator generator) {
        return new StreamingExecutionStrategy(generator);
    }

    private StreamingExecutionStrategy(JsonGenerator generator) {
        super();
        this.generator = generator;
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecutionContext context, ExecutionStrategyParameters parameters) {
        try {
            generator.writeStartObject();
            generator.writeFieldName(Bundle.DATA_KEY);
            executeStream(context, parameters);
            generator.writeEndObject();
            return Async.toCompletableFuture(new ExecutionResultImpl(null, context.getErrors()));
        } catch (IOException e) {
            return Async.exceptionallyCompletedFuture(e);
        }
    }

    public void executeStream(ExecutionContext context, ExecutionStrategyParameters parameters) throws IOException {
        InstrumentationExecutionStrategyParameters instrumentationParameters = new InstrumentationExecutionStrategyParameters(context, parameters);

        ExecutionStrategyInstrumentationContext executionStrategyCtx = context.getInstrumentation().beginExecutionStrategy(instrumentationParameters);

        MergedSelectionSet fields = parameters.getFields();

        generator.writeStartObject();
        for (String fieldName : fields.keySet()) {
            MergedField currentField = fields.getSubField(fieldName);
            ResultPath fieldPath = parameters.getPath().segment(mkNameForPath(currentField));
            ExecutionStrategyParameters newParameters = parameters
                    .transform(builder -> builder.field(currentField).path(fieldPath).parent(parameters));
            generator.writeFieldName(fieldName);

            final CompletableFuture<FetchedValue> completableFuture = fetchField(context, newParameters);
            FetchedValue fetchedValue = completableFuture.join();
            completeFieldStream(context, newParameters, fetchedValue);
        }
        generator.writeEndObject();

        executionStrategyCtx.onCompleted(null, null);
    }

    private void completeFieldStream(ExecutionContext context, ExecutionStrategyParameters parameters, FetchedValue fetchedValue) throws IOException {
        Field field = parameters.getField().getSingleField();
        GraphQLObjectType parentType = (GraphQLObjectType) parameters.getExecutionStepInfo().getUnwrappedNonNullType();
        GraphQLFieldDefinition fieldDef = getFieldDef(context.getGraphQLSchema(), parentType, field);
        ExecutionStepInfo executionStepInfo = createExecutionStepInfo(context, parameters, fieldDef, parentType);

        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(context, executionStepInfo);

        ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                builder.executionStepInfo(executionStepInfo)
                        .source(fetchedValue.getFetchedValue())
                        .localContext(fetchedValue.getLocalContext())
                        .nonNullFieldValidator(nonNullableFieldValidator)
        );

        completeValueStream(context, newParameters);
    }


    private void completeValueStream(ExecutionContext context, ExecutionStrategyParameters parameters) throws IOException {
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        final ValueUnboxer valueUnboxer = context.getValueUnboxer();
        GraphQLType fieldType = executionStepInfo.getUnwrappedNonNullType();
        Object result = valueUnboxer.unbox(parameters.getSource());

        if (result == null) {
            generator.writeNull();
        } else if (isList(fieldType)) {
            completeValueForListStream(context, parameters, result);
        } else if (isScalar(fieldType)) {
            completeValueForScalarStream(context, parameters, (GraphQLScalarType) fieldType, result);
        } else if (isEnum(fieldType)) {
            completeValueForEnumStream(context, parameters, (GraphQLEnumType) fieldType, result);
        } else {
            // when we are here, we have a complex type: Interface, Union or Object
            // and we must go deeper
            //
            GraphQLObjectType resolvedObjectType;
            try {
                resolvedObjectType = resolveType(context, parameters, fieldType);
                completeValueForObjectStream(context, parameters, resolvedObjectType, result);
            } catch (UnresolvedTypeException ex) {
                // consider the result to be null and add the error on the context
                handleUnresolvedTypeProblem(context, parameters, ex);
                // and validate the field is nullable, if non-nullable throw exception
                parameters.getNonNullFieldValidator().validate(parameters.getPath(), null);
            }
        }
    }

    protected void completeValueForObjectStream(ExecutionContext context, ExecutionStrategyParameters parameters, GraphQLObjectType resolvedObjectType, Object result) throws IOException {
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        FieldCollectorParameters collectorParameters = newParameters()
                .schema(context.getGraphQLSchema())
                .objectType(resolvedObjectType)
                .fragments(context.getFragmentsByName())
                .variables(context.getVariables())
                .build();

        final MergedField field = parameters.getField();
        MergedSelectionSet subFields = fieldCollector.collectFields(collectorParameters, field);

        ExecutionStepInfo newExecutionStepInfo = executionStepInfo.changeTypeWithPreservedNonNull(resolvedObjectType);
        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(context, newExecutionStepInfo);

        ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                builder.executionStepInfo(newExecutionStepInfo)
                        .fields(subFields)
                        .nonNullFieldValidator(nonNullableFieldValidator)
                        .source(result)
        );

        // Calling this from the executionContext to ensure we shift back from mutation strategy to the query strategy.
        executeStream(context, newParameters);
    }

    private void completeValueForEnumStream(ExecutionContext context, ExecutionStrategyParameters parameters, GraphQLEnumType enumType, Object result) throws IOException {
        Object serialized;
        try {
            serialized = enumType.serialize(result);
        } catch (CoercingSerializeException e) {
            serialized = handleCoercionProblem(context, parameters, e);
        }
        try {
            serialized = parameters.getNonNullFieldValidator().validate(parameters.getPath(), serialized);
        } catch (NonNullableFieldWasNullException e) {
            throw new IOException("NonNull field was null: " + parameters.getPath().getSegmentName());
        }
        generator.writeObject(serialized);
    }

    private void completeValueForScalarStream(ExecutionContext context, ExecutionStrategyParameters parameters, GraphQLScalarType scalarType, Object result) throws IOException {
        Object serialized;
        try {
            serialized = scalarType.getCoercing().serialize(result);
        } catch (CoercingSerializeException e) {
            serialized = handleCoercionProblem(context, parameters, e);
        }

        // TODO: fix that: this should not be handled here
        //6.6.1 http://facebook.github.io/graphql/#sec-Field-entries
        if (serialized instanceof Double && ((Double) serialized).isNaN()) {
            serialized = null;
        }
        try {
            serialized = parameters.getNonNullFieldValidator().validate(parameters.getPath(), serialized);
        } catch (NonNullableFieldWasNullException e) {
            throw new IOException("NonNull field was null: " + parameters.getPath().getSegmentName());
        }
        generator.writeObject(serialized);
    }

    private void completeValueForListStream(ExecutionContext context, ExecutionStrategyParameters parameters, Object result) throws IOException {
        Iterable<Object> resultIterable = toIterable(context, parameters, result);
        try {
            resultIterable = parameters.getNonNullFieldValidator().validate(parameters.getPath(), resultIterable);
            completeValueForListStream(context, parameters, resultIterable);
        } catch (NonNullableFieldWasNullException e) {
            throw new IOException("NonNull field was null: " + parameters.getPath().getSegmentName());
        }
    }

    private void completeValueForListStream(ExecutionContext context, ExecutionStrategyParameters parameters, Iterable<Object> interableValues) throws IOException {
        OptionalInt size = FpKit.toSize(interableValues);
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();

        generator.writeStartArray();
        int index = 0;
        for (Object item : interableValues) {
            index++;
            ResultPath indexedPath = parameters.getPath().segment(index);

            ExecutionStepInfo stepInfoForListElement = executionStepInfoFactory.newExecutionStepInfoForListElement(executionStepInfo, index);

            NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(context, stepInfoForListElement);

            int finalIndex = index;
            FetchedValue value = unboxPossibleDataFetcherResult(context, parameters, item);

            ExecutionStrategyParameters newParameters = parameters.transform(builder ->
                    builder.executionStepInfo(stepInfoForListElement)
                            .nonNullFieldValidator(nonNullableFieldValidator)
                            .listSize(size.orElse(-1)) // -1 signals that we don't know the size
                            .localContext(value.getLocalContext())
                            .currentListIndex(finalIndex)
                            .path(indexedPath)
                            .source(value.getFetchedValue())
            );
            completeValueStream(context, newParameters);
            index++;
        }
        generator.writeEndArray();
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
