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
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.project.ws.GenericResource;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.project.ws.GenericResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GenericResourceClientTest extends AbstractResourceClientTest {

    private static final String ITEM1 = "c1";
    private static final String ITEM2 = "c4";
    private static final String ITEM1_PID = ITEM1 + "-12345678";
    private static final String BAD_ITEM = "cd1";
    private static final String PRIVILEGED_USER_NAME = "mike";
    private static final String LIMITED_USER_NAME = "reto";

    @Test
    public void getMultipleGenericEntities() throws IOException {
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("id", ITEM1)
                .queryParam("id", ITEM2).build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getMultipleGenericEntitiesByPost() throws IOException {
        String payload = String.format("[\"%s\", \"%s\"]", ITEM1, ITEM2);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), ehriUri(ENDPOINT))
                .entity(payload)
                .post(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getGenericEntitiesByPid() throws IOException {
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("pid", ITEM1_PID).build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class), JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());

        // Test POST
        String payload = String.format("[{\"pid\": \"%s\"}]", ITEM1_PID);
        uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT).build();

        response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(payload)
                .post(ClientResponse.class);
        rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getGenericEntitiesByGid() throws IOException {
        String payload = String.format("[\"%s\"]", ITEM1);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), ehriUri(ENDPOINT))
                .entity(payload)
                .post(ClientResponse.class);
        assertStatus(OK, response);
        Long gid = getGraphId(response);

        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("gid", gid).build();

        response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());

        // Test POST
        payload = String.format("[%s]", gid);
        uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT).build();

        response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(payload)
                .post(ClientResponse.class);
        rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getSingleGenericEntity() throws IOException {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, ITEM1)).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getSingleGenericEntityByPid() throws IOException {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "pid:" + ITEM1_PID)).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getCannotFetchNonContentTypes() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, BAD_ITEM)).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void listEntitiesGivesNullForBadItems() throws Exception {
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("gid", -1L)  // bad GID
                .queryParam("id", "c1")  // not accessible
                .queryParam("id", "ur1") // non-content type
                .queryParam("id", "c4")  // OK
                .build();

        ClientResponse response = jsonCallAs(getRegularUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        System.out.println(rootNode.toString());
        assertTrue(rootNode.isArray());
        assertEquals(4, rootNode.size());
        assertTrue(rootNode.path(0).isNull());
        assertTrue(rootNode.path(1).isNull());
        assertTrue(rootNode.path(2).isNull());
        assertTrue(rootNode.path(3).isObject());
    }

    @Test
    public void testGetLinks() throws Exception {
        // Fetch annotations for an item.
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", "links"))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAnnotations() throws Exception {
        // Fetch annotations for an item.
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", "annotations"))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAccessors() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(ENDPOINT, "c1", "access")).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testUserCannotRead() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testGrantAccess() throws Exception {
        // Attempt to fetch an element.
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(NOT_FOUND, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        response = client.resource(ehriUri(ENDPOINT, "c1", "access"))
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testRevokeAccess() throws Exception {

        // first, grant access
        testGrantAccess();

        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(OK, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME);

        WebResource resource = client.resource(ehriUri(ENDPOINT, "c1", "access"));
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testDiff() throws Exception {
        // Create a version
        Bundle before = getEntity(Entities.REPOSITORY, "r1", getAdminUserProfileId());
        String jsonAgentTestString = "{\"type\": \"Repository\", \"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.REPOSITORY, "r1"))
                .entity(jsonAgentTestString)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        List<Bundle> versions = getItemList(ehriUri(GenericResource.ENDPOINT, "r1", "versions"),
                getAdminUserProfileId());
        assertEquals(1, versions.size());

        ClientResponse response2 = jsonCallAs(getAdminUserProfileId(),
                ehriUri(GenericResource.ENDPOINT, "r1", "diff", versions.get(0).getId()))
                .get(ClientResponse.class);
        assertStatus(OK, response2);
        JsonNode out = jsonMapper.readTree(response2.getEntity(String.class));
        // NB: If this starts failing it's due to JsonDiff output not being
        // consistently ordered.
        assertEquals("replace", out.path(0).path("op").textValue());
        assertEquals("/data/identifier", out.path(0).path("path").textValue());
        assertEquals("jmp", out.path(0).path("value").textValue());
        assertEquals("remove", out.path(1).path("op").textValue());
        assertEquals("/data/name", out.path(1).path("path").textValue());
        assertEquals("remove", out.path(2).path("op").textValue());
        assertEquals("/relationships/describes", out.path(2).path("path").textValue());

    }


    private void testResponse(ClientResponse response, String expectedId) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(expectedId, idValue.textValue());
        // ensure only one item was returned...
        assertFalse(rootNode.path(1).isMissingNode());
        assertTrue(rootNode.path(2).isMissingNode());
    }

    private Long getGraphId(ClientResponse response) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.META_KEY).path("gid");
        return idValue.asLong();
    }
}
