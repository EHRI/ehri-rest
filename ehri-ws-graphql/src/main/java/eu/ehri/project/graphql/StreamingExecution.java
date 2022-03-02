/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import graphql.ExecutionInput;
import graphql.execution.Execution;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionContextBuilder;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStrategy;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.ExecutionTypeInfo;
import graphql.execution.FieldCollector;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.NonNullableFieldValidator;
import graphql.execution.instrumentation.Instrumentation;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.NodeUtil;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.execution.ExecutionTypeInfo.newTypeInfo;
import static graphql.execution.FieldCollectorParameters.newParameters;
import static graphql.language.OperationDefinition.Operation.MUTATION;
import static graphql.language.OperationDefinition.Operation.QUERY;
import static graphql.language.OperationDefinition.Operation.SUBSCRIPTION;


public class StreamingExecution extends Execution {

    private final FieldCollector fieldCollector = new FieldCollector();
    private final ExecutionStrategy queryStrategy;
    private final ExecutionStrategy mutationStrategy;
    private final ExecutionStrategy subscriptionStrategy;
    private final Instrumentation instrumentation;

    public StreamingExecution(ExecutionStrategy queryStrategy, ExecutionStrategy mutationStrategy, ExecutionStrategy subscriptionStrategy, Instrumentation instrumentation) {
        super(queryStrategy, mutationStrategy, subscriptionStrategy, instrumentation);
        this.queryStrategy = queryStrategy;
        this.mutationStrategy = mutationStrategy;
        this.subscriptionStrategy = subscriptionStrategy;
        this.instrumentation = instrumentation;
    }

    private GraphQLObjectType getOperationRootType(GraphQLSchema graphQLSchema, OperationDefinition.Operation operation) {
        if (operation == MUTATION) {
            return graphQLSchema.getMutationType();
        } else if (operation == QUERY) {
            return graphQLSchema.getQueryType();
        } else if (operation == SUBSCRIPTION) {
            return graphQLSchema.getSubscriptionType();
        } else {
            return assertShouldNeverHappen("Unhandled case.  An extra operation enum has been added without code support");
        }
    }
    private void executeOperation(
            JsonGenerator generator,
            ExecutionContext executionContext,
            Object root,
            OperationDefinition operationDefinition) throws IOException {
        GraphQLObjectType operationRootType = getOperationRootType(executionContext.getGraphQLSchema(),
                operationDefinition.getOperation());

        FieldCollectorParameters collectorParameters = newParameters()
                .schema(executionContext.getGraphQLSchema())
                .objectType(operationRootType)
                .fragments(executionContext.getFragmentsByName())
                .variables(executionContext.getVariables())
                .build();

        Map<String, List<Field>> fields = fieldCollector.collectFields(collectorParameters, operationDefinition.getSelectionSet());

        ExecutionTypeInfo typeInfo = newTypeInfo().type(operationRootType).build();
        NonNullableFieldValidator nonNullableFieldValidator = new NonNullableFieldValidator(executionContext, typeInfo);

        ExecutionStrategyParameters parameters = ExecutionStrategyParameters.newParameters()
                .typeInfo(typeInfo)
                .source(root)
                .fields(fields)
                .nonNullFieldValidator(nonNullableFieldValidator)
                .path(ExecutionPath.rootPath())
                .build();

        new StreamingExecutionStrategy().execute(generator, executionContext, parameters);
    }

    public void execute(JsonGenerator generator, GraphQLSchema graphQLSchema, Document document, ExecutionId executionId, ExecutionInput executionInput) throws IOException {
        NodeUtil.GetOperationResult operationResult = NodeUtil.getOperation(document, executionInput.getOperationName());
        ExecutionContext executionContext = new ExecutionContextBuilder()
                .instrumentation(instrumentation)
                .executionId(executionId)
                .graphQLSchema(graphQLSchema)
                .queryStrategy(queryStrategy)
                .mutationStrategy(mutationStrategy)
                .subscriptionStrategy(subscriptionStrategy)
                .operationDefinition(operationResult.operationDefinition)
                .context(executionInput.getContext())
                .fragmentsByName(operationResult.fragmentsByName)
                .root(executionInput.getRoot())
                .document(document)
                .variables(executionInput.getVariables())
                .build();

        executeOperation(generator, executionContext, executionInput.getRoot(), executionContext.getOperationDefinition());
    }
}
