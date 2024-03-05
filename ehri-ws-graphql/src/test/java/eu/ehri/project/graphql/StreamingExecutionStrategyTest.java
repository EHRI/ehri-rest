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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ehri.project.test.AbstractFixtureTest;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.schema.GraphQLSchema;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


public class StreamingExecutionStrategyTest extends AbstractFixtureTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonNode executeStream(String query, Map<String, Object> params) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator generator = mapper.getFactory().createGenerator(out)
                .useDefaultPrettyPrinter()) {
            GraphQLSchema schema = new GraphQLImpl(api(adminUser), true).getSchema();

            final StreamingExecutionStrategy strategy = StreamingExecutionStrategy.jsonGenerator(generator);
            final ExecutionInput input = ExecutionInput.newExecutionInput()
                    .query(query)
                    .operationName(null)
                    .variables(params)
                    .build();

            final GraphQL graphQL = GraphQL
                    .newGraphQL(schema)
                    .queryExecutionStrategy(strategy)
                    .instrumentation(new MaxQueryDepthInstrumentation(5))
                    .build();

            graphQL.execute(input);

        }
        return mapper.readTree(out.toByteArray());
    }

    @Test
    public void testExecuteWithErrors() throws Exception {
        String testQuery = readResourceFileAsString("testquery-bad.graphql");
        JsonNode json = executeStream(testQuery, Collections.emptyMap());
        assertEquals("", json.toPrettyString());
    }

    @Test
    public void textExecuteMaxDepth() throws Exception {
        String testQuery = readResourceFileAsString("testquery-depth20.graphql");
        JsonNode json = executeStream(testQuery, Collections.emptyMap());
        // System.out.println("JSON: " + json);
        assertEquals("", json.toPrettyString());
    }

    @Test
    public void textExecute() throws Exception {
        String testQuery = readResourceFileAsString("testquery-connection.graphql");
        JsonNode json = executeStream(testQuery, Collections.emptyMap());
        // System.out.println("JSON: " + json);
        String first = json.path("data").path("firstTwo").path("items").path(0).path("id").textValue();
        assertThat(first, anyOf(containsString("c1"), containsString("nl-r1-m19")));
    }
}
