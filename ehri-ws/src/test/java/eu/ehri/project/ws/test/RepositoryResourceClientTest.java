/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.ws.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.ws.PermissionsResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class RepositoryResourceClientTest extends AbstractResourceClientTest {

    private static final String COUNTRY_CODE = "nl";
    private static final String ID = "r1";
    private static final String LIMITED_USER_NAME = "reto";
    private static final String UPDATED_NAME = "UpdatedNameTEST";

    private String agentTestData;
    private String docTestData;

    @Before
    public void setUp() throws Exception {
        agentTestData = readResourceFileAsString("Repository.json");
        docTestData = readResourceFileAsString("DocumentaryUnit.json");
    }

    @Test
    public void testCreateRepository() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.COUNTRY, COUNTRY_CODE))
                .entity(agentTestData)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateRepositoryWithExistingIdentifier() throws Exception {
        String json = Bundle.fromString(agentTestData)
                .withDataValue(Ontology.IDENTIFIER_KEY, "r1").toJson();
        URI uri = entityUri(Entities.COUNTRY, COUNTRY_CODE);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                uri).entity(json)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Now do it again!
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(json)
                .post(ClientResponse.class);
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testUpdateRepositoryByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.COUNTRY, COUNTRY_CODE))
                .entity(agentTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Obtain some update data.
        String updateData = Bundle.fromString(agentTestData)
                .withDataValue("name", UPDATED_NAME).toJson();

        response = jsonCallAs(getAdminUserProfileId(),
                response.getLocation()).entity(updateData)
                .put(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateRepositoryWithDeserializationError() throws Exception {
        // Create
        String badRepositoryTestData = "{\"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.COUNTRY, COUNTRY_CODE))
                .entity(badRepositoryTestData)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);

        // Check the JSON gives use the correct error
        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode errValue = rootNode.path("error");
        assertFalse(errValue.isMissingNode());
        assertEquals(BAD_REQUEST.getReasonPhrase(), errValue.asText());
    }

    @Test
    public void testDeleteRepository() throws Exception {
        // Create
        URI uri = entityUri(Entities.REPOSITORY, "r2");
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);

        assertStatus(NO_CONTENT, response);

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);

        assertStatus(GONE, response);
    }

    @Test
    public void testExportEad() throws Exception {
        // Create
        URI uri = entityUri(Entities.REPOSITORY, ID, "ead");
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        try (InputStream stream = response.getEntityInputStream()) {
            // There should be three top level items: c1, c4, and m19
            assertEquals(3, readZip(stream).size());
        }
    }

    @Test
    public void testGrantPermsForRepositoryScope() throws Exception {
        // Grant permissions for a user to create items within this scope.

        // The user shouldn't be able to create docs with r2
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                getCreationUriFor("r2")).entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(FORBIDDEN, response);

        // Or r3...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r3"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(FORBIDDEN, response);

        // Now grant the user permissions to create just within
        // the scope of r2
        String permData = "{\"DocumentaryUnit\": [\"create\"]}";

        URI grantUri = ehriUri(PermissionsResource.ENDPOINT, LIMITED_USER_NAME, "scope", "r2");

        response = jsonCallAs(getAdminUserProfileId(), grantUri)
                .entity(permData)
                .post(ClientResponse.class);

        assertStatus(OK, response);

        // Now creation should succeed...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r2"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // But r3 should still fail...
        // Or r3...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r3"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(FORBIDDEN, response);

        // And the user himself should not be able to grant
        // others the ability to create within that scope.
        String otherUserName = "linda";
        String grantPermData = "{\"DocumentaryUnit\": [\"grant\"]}";
        URI otherGrantUri = ehriUri(PermissionsResource.ENDPOINT, otherUserName, "scope", "r2");

        response = jsonCallAs(LIMITED_USER_NAME, otherGrantUri)
                .entity(grantPermData)
                .post(ClientResponse.class);

        assertStatus(FORBIDDEN, response);
    }

    private URI getCreationUriFor(String id) {
        return entityUri(Entities.REPOSITORY, id);
    }
}
