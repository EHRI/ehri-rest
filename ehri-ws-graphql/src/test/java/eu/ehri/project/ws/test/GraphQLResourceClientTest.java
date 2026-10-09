/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.ws.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import eu.ehri.project.ws.GraphQLResource;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.ws.providers.GraphQLQueryProvider;
import eu.ehri.project.graphql.GraphQLQuery;
import eu.ehri.project.persistence.Bundle;
import graphql.introspection.IntrospectionQuery;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Map;

import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Test for the GraphQL endpoint
 */
public class GraphQLResourceClientTest extends AbstractResourceClientTest {

    @Before
    public void setUp() {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(GraphQLQueryProvider.class);
        config.getClasses().add(JacksonFeatures.class);
        client = Client.create(config);
    }

    @Test
    public void testGraphQLSchema() throws Exception {
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .get(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertFalse(data.path("data").path("__schema").isMissingNode());
    }

    @Test
    public void testGraphQLSchemaIntrospection() throws Exception {
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getRegularUserProfileId(), queryUri)
                .entity(new GraphQLQuery(IntrospectionQuery.INTROSPECTION_QUERY), MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertFalse(data.path("data").path("__schema").isMissingNode());
    }

    @Test
    public void testGraphQLQuery() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(testQuery)
                .post(ClientResponse.class);

        // Without the X-Stream header we should get strict execution.
        assertNull(response.getHeaders().getFirst("Transfer-Encoding"));

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        // A light end-to-end check: field-level assertions on the
        // query results live in GraphQLImplTest.
        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertEquals("c1-12345678", data.path("data").path("c1").path("pid").textValue());
    }

    @Test
    public void testGraphQLQueryWithStandardPerms() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getRegularUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery), MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        // c1 should be missing since we can't read this item
        assertTrue(data.path("data").path("c1").isNull());
        // the annotation should be missing since its not accessible
        assertTrue(data.path("data").path("c4")
                .path("annotations").path(0).path("id").isMissingNode());
    }

    @Test
    public void testGraphQLQueryViaJson() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery), MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("c1", data.path("data").path("c1")
                .path(Bundle.ID_KEY).textValue());
    }

    @Test
    public void testGraphQLQueryViaJsonWithError() throws Exception {

        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity("{bad-json]", MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("JsonError", data.path("errors").path(0).path("type").textValue());
    }

    @Test
    public void testGraphQLQueryErrors() throws Exception {
        String testQuery = readResourceFileAsString("testquery-bad.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(testQuery)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("Validation error (MissingFieldArgument@[DocumentaryUnit]) : Missing field argument 'id'",
                data.path("errors").path(0).path("message").textValue());
    }

    @Test
    public void testGraphQLBadQueryVariable() throws Exception {
        String testQuery = readResourceFileAsString("testquery-variables.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Map<String, Object> vars = Maps.newHashMap();
        vars.put("n", 1234567891011L);

        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery, vars, null))
                .post(ClientResponse.class);
        //System.out.println(response.getEntity(String.class));
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testGraphQLNullQueryVariable() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();

        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery, null, null))
                .post(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGraphQLStreaming() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .header(AbstractResource.STREAM_HEADER_NAME, "true")
                .entity(testQuery)
                .post(ClientResponse.class);
        //System.out.println(response.getEntity(String.class));
        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("chunked", response.getHeaders().getFirst("Transfer-Encoding"));
        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertFalse(data.path("data").path("topLevelDocumentaryUnits").path("items").path(0).isMissingNode());
    }

    @Test
    public void testGraphQLStreamingWithError() throws Exception {
        String testQuery = readResourceFileAsString("testquery-bad.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .header(AbstractResource.STREAM_HEADER_NAME, "true")
                .entity(testQuery)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("Validation error (MissingFieldArgument@[DocumentaryUnit]) : Missing field argument 'id'",
                data.path("errors").path(0).path("message").textValue());
    }

    @Test
    public void testGraphQLExceedingMaxComplexity() throws Exception {
        String testQuery = readResourceFileAsString("testquery-depth20.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = client.resource(queryUri)
                .entity(testQuery)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.getEntity(JsonNode.class);
        assertEquals("maximum query depth exceeded 20 > 15",
                data.path("errors").path(0).path("message").textValue());
    }
}
