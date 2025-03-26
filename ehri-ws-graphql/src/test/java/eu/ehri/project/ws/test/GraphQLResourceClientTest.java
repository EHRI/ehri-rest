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
import eu.ehri.project.graphql.GraphQLQuery;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.ws.GraphQLResource;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.ws.providers.GraphQLQueryProvider;
import graphql.introspection.IntrospectionQuery;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;


/**
 * Test for the GraphQL endpoint
 */
public class GraphQLResourceClientTest extends AbstractResourceClientTest {

    @Before
    public void setUp() {
        ClientConfig config = new ClientConfig();
        config.register(GraphQLQueryProvider.class);
        config.register(JacksonFeatures.class);
        client = ClientBuilder.newClient(config);
    }

    @Test
    public void testGraphQLSchema() throws Exception {
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .get(Response.class);

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertFalse(data.path("data").path("__schema").isMissingNode());
    }

    @Test
    public void testGraphQLSchemaIntrospection() throws Exception {
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getRegularUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(IntrospectionQuery.INTROSPECTION_QUERY)), Response.class);

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertFalse(data.path("data").path("__schema").isMissingNode());
    }

    @Test
    public void testGraphQLQuery() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.text(testQuery), Response.class);

        // Without the X-Stream header we should get strict execution.
        assertNull(response.getHeaders().getFirst("Transfer-Encoding"));

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
//         System.out.println(data.toPrettyString());
        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertEquals(0, data.path("data").path("c1").path("ancestors").size());
        assertEquals(1, data.path("data").path("c1")
                .path("children").path("items").size());
        assertEquals(2, data.path("data").path("c1")
                .path("allChildren").path("items").size());
        assertFalse(data.path("data").path("c1").path("itemCount").isMissingNode());
        assertEquals(1, data.path("data").path("c1").path("itemCount").intValue());
        assertEquals("c1-alt", data.path("data").path("c1").path("otherIdentifiers").path(0).textValue());
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
        assertEquals("An Address", data.path("data").path("c1").path("repository")
                .path("english").path("addresses").path(0)
                .path("addressName").textValue());
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
        // System.out.println(data.path("data").path("r4").path("links").path(0).path("targets").toPrettyString());
        assertThat(data.path("data").path("r4").path("links")
                .path(0).path("targets").path(0).path("id").textValue()).isIn("c4", "r4");
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
        Response response = callAs(getRegularUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery)), Response.class);

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
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
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery)), Response.class);

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertEquals("c1", data.path("data").path("c1")
                .path(Bundle.ID_KEY).textValue());
    }

    @Test
    public void testGraphQLQueryViaJsonWithError() throws Exception {

        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json("{bad-json}"), Response.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertEquals("JsonError", data.path("errors").path(0).path("type").textValue());
    }

    @Test
    public void testGraphQLQueryErrors() throws Exception {
        String testQuery = readResourceFileAsString("testquery-bad.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.text(testQuery), Response.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertEquals("Validation error (MissingFieldArgument@[DocumentaryUnit]) : Missing field argument 'id'",
                data.path("errors").path(0).path("message").textValue());
    }

    @Test
    public void testGraphQLQueryConnection() throws Exception {
        String testQuery = readResourceFileAsString("testquery-connection.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.text(testQuery), Response.class);

        JsonNode data = response.readEntity(JsonNode.class);
        System.out.println(data);
        assertStatus(OK, response);

        assertTrue(data.path("data").path("empty").path("pageInfo").path("hasPreviousPage").asBoolean());
        assertFalse(data.path("data").path("empty").path("pageInfo").path("hasNextPage").asBoolean());
    }

    @Test
    public void testGraphQLQueryVariables() throws Exception {
        String testQuery = readResourceFileAsString("testquery-variables.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Map<String, Object> vars = Maps.newHashMap();
        vars.put("n", 4);

        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery, vars, null)), Response.class);

        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
        //System.out.println(data);
        assertEquals(4, data.path("data").path("test").path("items").size());
        assertFalse(data.path("data").path("test").path("pageInfo").path("nextPage").isNull());
        assertStatus(OK, response);

        vars.put("from", data.path("data").path("test").path("pageInfo").path("nextPage").textValue());
        Response nextResponse = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery, vars, null)), Response.class);
        JsonNode nextData = nextResponse.readEntity(JsonNode.class);
        assertEquals(1, nextData.path("data").path("test").path("items").size());
        assertStatus(OK, nextResponse);
    }

    @Test
    public void testGraphQLBadQueryVariable() throws Exception {
        String testQuery = readResourceFileAsString("testquery-variables.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Map<String, Object> vars = Maps.newHashMap();
        vars.put("n", 1234567891011L);

        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery, vars, null)), Response.class);
        //System.out.println(response.readEntity(String.class));
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testGraphQLNullQueryVariable() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();

        Response response = callAs(getAdminUserProfileId(), queryUri)
                .post(Entity.json(new GraphQLQuery(testQuery, null, null)), Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGraphQLStreaming() throws Exception {
        String testQuery = readResourceFileAsString("testquery.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .header(AbstractResource.STREAM_HEADER_NAME, "true")
                .post(Entity.text(testQuery), Response.class);
        //System.out.println(response.readEntity(String.class));
        assertStatus(OK, response);
        JsonNode data = response.readEntity(JsonNode.class);
        // FIXME: Neo4j 4: no longer getting the Transfer-Encoding, despite using a StreamingOutput
        // might be to do with the Jersey impl used by Neo4j?
        //assertEquals("chunked", response.getHeaders().getFirst("Transfer-Encoding"));
        //assertEquals("chunked", response.getHeaders().getFirst("Transfer-Encoding"));
        // System.out.println(data);
        assertEquals("c1", data.path("data").path("c1").path("id").textValue());
        assertFalse(data.path("data").path("topLevelDocumentaryUnits").path("items").path(0).isMissingNode());
    }

    @Test
    public void testGraphQLStreamingWithError() throws Exception {
        String testQuery = readResourceFileAsString("testquery-bad.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = callAs(getAdminUserProfileId(), queryUri)
                .header(AbstractResource.STREAM_HEADER_NAME, "true")
                .post(Entity.text(testQuery), Response.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertEquals("Validation error (MissingFieldArgument@[DocumentaryUnit]) : Missing field argument 'id'",
                data.path("errors").path(0).path("message").textValue());
    }

    @Test
    public void testGraphQLExceedingMaxComplexity() throws Exception {
        String testQuery = readResourceFileAsString("testquery-depth20.graphql");
        URI queryUri = ehriUriBuilder(GraphQLResource.ENDPOINT).build();
        Response response = client.target(queryUri)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(testQuery), Response.class);

        assertStatus(BAD_REQUEST, response);
        JsonNode data = response.readEntity(JsonNode.class);
        assertEquals("maximum query depth exceeded 20 > 15",
                data.path("errors").path(0).path("message").textValue());
    }
}
