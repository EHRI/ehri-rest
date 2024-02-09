/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
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
//         System.out.println(data.toPrettyString());
        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertEquals(0, data.path("data").path("c1").path("ancestors").size());
        assertEquals(1, data.path("data").path("c1")
                .path("children").path("items").size());
        assertEquals(2, data.path("data").path("c1")
                .path("allChildren").path("items").size());
        assertFalse(data.path("data").path("c1").path("itemCount").isMissingNode());
        assertEquals(1, data.path("data").path("c1").path("itemCount").intValue());
        assertEquals(0, data.path("data").path("c4").path("itemCount").intValue());
        assertEquals("c2", data.path("data").path("c1")
                .path("children").path("items").path(0)
                .path("id").textValue());
        assertEquals("c3", data.path("data").path("c1")
                .path("children").path("items").path(0)
                .path("children").path("items").path(0)
                .path("id").textValue());
        assertEquals("a2", data.path("data").path("c3").path("related")
                .path(0).path("item").path("id").textValue());
        assertEquals("ur3", data.path("data").path("c3").path("related")
                .path(0).path("context").path("body").path(0).path("id").textValue());
        assertEquals("Person Access 2", data.path("data").path("c3").path("related")
                .path(0).path("context").path("body").path(0).path("name").textValue());
        assertEquals("Amsterdam", data.path("data").path("c1").path("repository")
                .path("english").path("addresses").path(0)
                .path("municipality").textValue());
        assertEquals("test@example.com", data.path("data").path("c1")
                .path("repository").path("english").path("addresses").path(0)
                .path("email").path(0).textValue());
        assertEquals(2, data.path("data").path("c3").path("ancestors").size());
        assertEquals("c2", data.path("data").path("c3").path("ancestors")
                .path(0).path("id").textValue());
        assertEquals("c1", data.path("data").path("c3").path("ancestors")
                .path(1).path("id").textValue());
        assertEquals("ann7", data.path("data").path("c4")
                .path("annotations").path(0).path("id").textValue());
        assertEquals("scopeAndContent", data.path("data").path("c3")
                .path("annotations").path(0).path("field").textValue());
        assertEquals("Mike", data.path("data").path("c3")
                .path("annotations").path(0).path("by").textValue());
        assertFalse(data.path("data").path("topLevelOnly")
                .path("items").path(0).path("id").isMissingNode());
        assertEquals(3, data.path("data").path("topLevelOnly")
                .path("items").size());
        assertEquals(5, data.path("data").path("allLevels")
                .path("items").size());
        assertFalse(data.path("data").path("topLevelDocumentaryUnits")
                .path("items").path(0).path("id").isMissingNode());
        assertEquals("c4", data.path("data").path("r4").path("links")
                .path(0).path("targets").path(0).path("id").textValue());
        assertEquals("cvocc1", data.path("data").path("cvocc2").path("related")
                .path(0).path("id").textValue());
        assertEquals("Subject Access 2", data.path("data").path("cvocc2").path("connected")
                .path(0).path("context").path("body").path(0).path("name").textValue());
        assertEquals("Test", data.path("data").path("gb").path("summary").textValue());
        assertEquals("Test", data.path("data").path("gb").path("situation").textValue());
        assertEquals("Test", data.path("data").path("gb").path("history").textValue());
        assertEquals("Test", data.path("data").path("gb").path("extensive").textValue());
        assertFalse(data.path("data").path("wrongType").isMissingNode());
        assertTrue(data.path("data").path("wrongType").isNull());
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
    public void testGraphQLQueryConnection() throws Exception {
        String testQuery = readResourceFileAsString("testquery-connection.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(testQuery)
                .post(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        System.out.println(data);

        assertTrue(data.path("data").path("empty").path("pageInfo").path("hasPreviousPage").asBoolean());
        assertFalse(data.path("data").path("empty").path("pageInfo").path("hasNextPage").asBoolean());
    }

    @Test
    public void testGraphQLQueryVariables() throws Exception {
        String testQuery = readResourceFileAsString("testquery-variables.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Map<String, Object> vars = Maps.newHashMap();
        vars.put("n", 4);

        ClientResponse response = callAs(getAdminUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery, vars, null))
                .post(ClientResponse.class);

        assertStatus(OK, response);
        JsonNode data = response.getEntity(JsonNode.class);
        //System.out.println(data);
        assertEquals(4, data.path("data").path("test").path("items").size());
        assertFalse(data.path("data").path("test").path("pageInfo").path("nextPage").isNull());
        assertStatus(OK, response);

        vars.put("from", data.path("data").path("test").path("pageInfo").path("nextPage").textValue());
        ClientResponse nextResponse = callAs(getAdminUserProfileId(), queryUri)
                .entity(new GraphQLQuery(testQuery, vars, null))
                .post(ClientResponse.class);
        JsonNode nextData = nextResponse.getEntity(JsonNode.class);
        assertEquals(1, nextData.path("data").path("test").path("items").size());
        assertStatus(OK, nextResponse);
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
