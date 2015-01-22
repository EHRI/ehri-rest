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

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.GenericResource.GID_PARAM;
import static eu.ehri.extension.GenericResource.ID_PARAM;
import static eu.ehri.extension.GenericResource.STRICT_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static eu.ehri.extension.GenericResource.ENDPOINT;

public class GenericRestClientTest extends BaseRestClientTest {

    private static final String ITEM1 = "c1";
    private static final String ITEM2 = "c4";
    private static final String BAD_ITEM = "cd1";

    @Test
    public void getMultipleGenericEntities() throws IOException {
        // Create
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .queryParam(ID_PARAM, ITEM1)
                .queryParam(ID_PARAM, ITEM2).build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getMultipleGenericEntitiesByPost() throws IOException {
        // Create
        String payload = String.format("[\"%s\", \"%s\"]", ITEM1, ITEM2);
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT).build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(payload)
                .post(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getSingleGenericEntity() throws IOException {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, ITEM1)).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.getTextValue());
    }

    @Test
    @Ignore
    public void getCannotFetchNonContentTypes() throws IOException {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, BAD_ITEM)).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void listEntitiesByGidThrows404WhenStrict() throws IOException {
        // Create
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .segment("listByGraphId")
                .queryParam(GID_PARAM, -1L)
                .queryParam(STRICT_PARAM, true).build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void listEntitiesByGidIsTolerant() throws IOException {
        // Create
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment(ENDPOINT)
                .segment("listByGraphId")
                .queryParam(GID_PARAM, -1L).build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        assertTrue(rootNode.isArray());
        assertTrue(rootNode.path(0).isMissingNode());
    }

    private void testResponse(ClientResponse response, String expectedId) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(expectedId, idValue.getTextValue());
        // ensure only one item was returned...
        assertFalse(rootNode.path(1).isMissingNode());
        assertTrue(rootNode.path(2).isMissingNode());
    }
}
