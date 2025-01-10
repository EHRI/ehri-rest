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
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;

import static eu.ehri.project.ws.GenericResource.ENDPOINT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;


public class GenericResourceClientTest extends AbstractResourceClientTest {

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
        Response response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(Response.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getMultipleGenericEntitiesByPost() throws IOException {
        String payload = String.format("[\"%s\", \"%s\"]", ITEM1, ITEM2);
        Response response = jsonCallAs(getAdminUserProfileId(), ehriUri(ENDPOINT))
                .post(Entity.json(payload), Response.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getGenericEntitiesByGid() throws IOException {
        String payload = String.format("[\"%s\"]", ITEM1);
        Response response = jsonCallAs(getAdminUserProfileId(), ehriUri(ENDPOINT))
                .post(Entity.json(payload), Response.class);
        assertStatus(OK, response);
        Long gid = getGraphId(response);

        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam("gid", gid).build();

        response = jsonCallAs(getAdminUserProfileId(), uri).get(Response.class);
        JsonNode rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());

        // Test POST
        payload = String.format("[%s]", gid);
        uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT).build();

        response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(payload), Response.class);
        rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
        idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getSingleGenericEntity() throws IOException {
        Response response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, ITEM1)).get(Response.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.textValue());
    }

    @Test
    public void getCannotFetchNonContentTypes() throws Exception {
        Response response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, BAD_ITEM)).get(Response.class);
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

        Response response = jsonCallAs(getRegularUserProfileId(), uri).get(Response.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
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
        Response response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", "links"))
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAnnotations() throws Exception {
        // Fetch annotations for an item.
        Response response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c1", "annotations"))
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testGetAccessors() throws Exception {
        // Create
        Response response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(ENDPOINT, "c1", "access")).get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testUserCannotRead() throws Exception {
        // Create
        Response response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(Response.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testGrantAccess() throws Exception {
        // Attempt to fetch an element.
        Response response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(Response.class);

        assertStatus(NOT_FOUND, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add(AbstractResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        WebTarget resource = client.target(ehriUri(ENDPOINT, "c1", "access"));
        for (String param : queryParams.keySet()) {
            resource = resource.queryParam(param, queryParams.getFirst(param));
        }
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(Entity.json(""), Response.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testRevokeAccess() throws Exception {

        // first, grant access
        testGrantAccess();

        // Create
        Response response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(Response.class);

        assertStatus(OK, response);

        WebTarget resource = client.target(ehriUri(ENDPOINT, "c1", "access"));
        response = resource
                .queryParam(AbstractResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(Entity.json(""), Response.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                entityUri(Entities.DOCUMENTARY_UNIT, "c1")).get(Response.class);
        assertStatus(NOT_FOUND, response);
    }


    private void testResponse(Response response, String expectedId) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(expectedId, idValue.textValue());
        // ensure only one item was returned...
        assertFalse(rootNode.path(1).isMissingNode());
        assertTrue(rootNode.path(2).isMissingNode());
    }

    private Long getGraphId(Response response) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.readEntity(String.class), JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.META_KEY).path("gid");
        return idValue.asLong();
    }
}
