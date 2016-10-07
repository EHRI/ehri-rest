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
import graphql.GraphQLException;
import graphql.execution.Execution;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionContextBuilder;
import graphql.execution.ExecutionStrategy;
import graphql.execution.FieldCollector;
import graphql.execution.ValuesResolver;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StreamingExecution extends Execution {

    private FieldCollector fieldCollector = new FieldCollector();
    private ExecutionStrategy strategy;

    public StreamingExecution(ExecutionStrategy strategy) {
        super(strategy);
        this.strategy = strategy;
    }

    private GraphQLObjectType getOperationRootType(GraphQLSchema graphQLSchema, OperationDefinition operationDefinition) {
        if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
            return graphQLSchema.getMutationType();

        } else if (operationDefinition.getOperation() == OperationDefinition.Operation.QUERY) {
            return graphQLSchema.getQueryType();

        } else {
            throw new GraphQLException();
        }
    }

    private void executeOperation(
            JsonGenerator generator,
            ExecutionContext executionContext,
            Object root,
            OperationDefinition operationDefinition) throws IOException {
        GraphQLObjectType operationRootType = getOperationRootType(executionContext.getGraphQLSchema(), operationDefinition);

        Map<String, List<Field>> fields = new LinkedHashMap<String, List<Field>>();
        fieldCollector.collectFields(executionContext, operationRootType, operationDefinition.getSelectionSet(), new ArrayList<String>(), fields);

        new StreamingExecutionStrategy().execute(generator, executionContext, operationRootType, root, fields);
    }

    public void execute(JsonGenerator generator, GraphQLSchema graphQLSchema, Object root, Document document, String operationName, Map<String, Object> arguments) throws IOException {
        ExecutionContextBuilder executionContextBuilder = new ExecutionContextBuilder(new ValuesResolver());
        ExecutionContext executionContext = executionContextBuilder.build(graphQLSchema, strategy, root, document, operationName, arguments);
        executeOperation(generator, executionContext, root, executionContext.getOperationDefinition());
    }
}
