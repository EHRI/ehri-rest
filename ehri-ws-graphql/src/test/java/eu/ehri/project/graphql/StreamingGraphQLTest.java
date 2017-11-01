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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ehri.project.test.AbstractFixtureTest;
import graphql.schema.GraphQLSchema;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StreamingGraphQLTest extends AbstractFixtureTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void textExecute() throws Exception {
        String testQuery = readResourceFileAsString("testquery-connection.graphql");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator generator = mapper.getFactory().createGenerator(out)
                .useDefaultPrettyPrinter()) {
            GraphQLSchema schema = new GraphQLImpl(manager, api(validUser), true).getSchema();
            StreamingGraphQL ql = new StreamingGraphQL(schema);
            ql.execute(generator, "test", testQuery, null, null, Collections.emptyMap());
        }
        JsonNode json = mapper.readTree(out.toByteArray());
        //System.out.println(json);
        assertEquals("c1", json.path("firstTwo").path("items")
                .path(0).path("id").textValue());
        assertEquals("c4", json.path("topLevelDocumentaryUnits").path("items")
                .path(0).path("id").textValue());
    }
}