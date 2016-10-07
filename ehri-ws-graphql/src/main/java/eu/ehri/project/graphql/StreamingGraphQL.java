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
import eu.ehri.extension.errors.ExecutionError;
import graphql.InvalidSyntaxError;
import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
//import org.antlr.v4.runtime.RecognitionException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;


public class StreamingGraphQL {

    private final GraphQLSchema graphQLSchema;

    public StreamingGraphQL(GraphQLSchema schema) {
        this.graphQLSchema = schema;
    }


    public void execute(JsonGenerator generator, Document document, String operationName, Object context, Map<String, Object>
            arguments) throws IOException {
        assertNotNull(arguments, "arguments can't be null");
        //log.info("Executing request. operation name: {}. Request: {} ", operationName, requestString);
        StreamingExecution execution = new StreamingExecution(new StreamingExecutionStrategy());
        execution.execute(generator, graphQLSchema, context, document, operationName, arguments);
    }

    public void execute(JsonGenerator generator, String requestString, String operationName, Object context, Map<String, Object> arguments) throws IOException {
        assertNotNull(arguments, "arguments can't be null");
        //log.info("Executing request. operation name: {}. Request: {} ", operationName, requestString);
        Document document = parseAndValidate(requestString);
        StreamingExecution execution = new StreamingExecution(new StreamingExecutionStrategy());
        execution.execute(generator, graphQLSchema, context, document, operationName, arguments);
    }

    public Document parseAndValidate(String requestString) {
        Parser parser = new Parser();
        Document document;
        try {
            document = parser.parseDocument(requestString);
        } catch (Exception e) {
            // FIXME: Antlr maven problems
            //RecognitionException recognitionException = (RecognitionException) e.getCause();
            //int line = recognitionException.getOffendingToken().getLine();
            //int positionInLine = recognitionException.getOffendingToken().getCharPositionInLine();
            SourceLocation sourceLocation = new SourceLocation(0, 0);
            InvalidSyntaxError invalidSyntaxError = new InvalidSyntaxError(sourceLocation);
            throw new ExecutionError(Collections.singletonList(invalidSyntaxError));
        }

        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);
        if (validationErrors.size() > 0) {
            throw new ExecutionError(validationErrors);
        }
        return document;
    }
}
