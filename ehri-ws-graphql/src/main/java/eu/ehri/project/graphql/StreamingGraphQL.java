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
import com.google.common.collect.Lists;
import eu.ehri.extension.errors.ExecutionError;
import graphql.ExecutionInput;
import graphql.GraphQLError;
import graphql.InvalidSyntaxError;
import graphql.execution.*;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.language.Document;
import graphql.language.NodeUtil;
import graphql.language.OperationDefinition;
import graphql.language.SourceLocation;
import graphql.language.VariableDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.antlr.v4.runtime.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;


public class StreamingGraphQL {

    private static final Logger log = LoggerFactory.getLogger(StreamingGraphQL.class);

    private final GraphQLSchema graphQLSchema;

    public StreamingGraphQL(GraphQLSchema schema) {
        this.graphQLSchema = schema;
    }


    public void execute(JsonGenerator generator, String requestString, Document document, String operationName, Object context, Map<String, Object>
            arguments) throws IOException {
        assertNotNull(arguments, () -> "arguments can't be null");
        log.trace("Executing request. operation name: {}. Request: {} ", operationName, document);
        StreamingExecution execution = new StreamingExecution(new StreamingExecutionStrategy(), new AsyncExecutionStrategy(),
                new SubscriptionExecutionStrategy(), SimpleInstrumentation.INSTANCE, ValueUnboxer.DEFAULT);
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .context(context)
                .variables(arguments)
                .query(requestString)
                .operationName(operationName)
                .build();
        execution.execute(generator, graphQLSchema, document, ExecutionId.from("test"), input);
    }

    public void execute(JsonGenerator generator, String requestString, String operationName, Object context, Map<String, Object> arguments) throws IOException {
        assertNotNull(arguments, () -> "arguments can't be null");
        log.trace("Executing request. operation name: {}. Request: {} ", operationName, requestString);
        Document document = parseAndValidate(requestString, operationName, arguments);
        execute(generator, requestString, document, operationName, context, arguments);
    }

    public Document parseAndValidate(String query, String operationName, Map<String, Object> variables) {
        Parser parser = new Parser();
        Document document;
        try {
            document = parser.parseDocument(query);
        } catch (Exception e) {
            RecognitionException recognitionException = (RecognitionException) e.getCause();
            SourceLocation sourceLocation = new SourceLocation(recognitionException.getOffendingToken().getLine(),
                    recognitionException.getOffendingToken().getCharPositionInLine());
            InvalidSyntaxError invalidSyntaxError = new InvalidSyntaxError(sourceLocation, "Invalid syntax");
            throw new ExecutionError(Collections.singletonList(invalidSyntaxError));
        }

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);
        if (validationErrors.size() > 0) {
            throw new ExecutionError(validationErrors);
        }

        NodeUtil.GetOperationResult operationResult = NodeUtil.getOperation(document, operationName);
        OperationDefinition operationDefinition = operationResult.operationDefinition;

        ValuesResolver valuesResolver = new ValuesResolver();
        List<VariableDefinition> variableDefinitions = operationDefinition.getVariableDefinitions();

        try {
            valuesResolver.coerceVariableValues(graphQLSchema, variableDefinitions, variables);
        } catch (RuntimeException rte) {
            if (rte instanceof GraphQLError) {
                throw new ExecutionError(Lists.newArrayList((GraphQLError)rte));
            }
            throw rte;
        }

        return document;
    }
}
