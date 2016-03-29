/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
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

package eu.ehri.extension.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.GenericResource;
import eu.ehri.extension.base.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.GenericResource.ACCESS;
import static eu.ehri.extension.GenericResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenericRestClientTest extends AbstractRestClientTest {

    private static final String ITEM1 = "c1";
    private static final String ITEM2 = "c4";
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
    public void getCannotFetchNonContentTypes() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, BAD_ITEM)).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void listEntitiesByGidThrows404() throws Exception {
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("gid", -1L).build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testGetLinks() throws Exception {
        // Fetch annotations for an item.
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", GenericResource.LINKS))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAnnotations() throws Exception {
        // Fetch annotations for an item.
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", GenericResource.ANNOTATIONS))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAccessors() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(ENDPOINT, "c1", ACCESS)).get(ClientResponse.class);
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
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        response = client.resource(ehriUri(ENDPOINT, "c1", ACCESS))
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME);

        WebResource resource = client.resource(ehriUri(ENDPOINT, "c1", ACCESS));
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
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
