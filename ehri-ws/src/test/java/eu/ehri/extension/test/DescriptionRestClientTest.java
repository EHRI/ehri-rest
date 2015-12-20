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
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.DescriptionResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DescriptionRestClientTest extends BaseRestClientTest {

    private String descriptionTestStr;
    private String accessPointTestStr;
    static final String TEST_DESCRIPTION_IDENTIFIER = "another-description";

    @Before
    public void setUp() throws Exception {
        descriptionTestStr = readResourceFileAsString("DocumentaryUnitDescription.json");
        accessPointTestStr = readResourceFileAsString("AccessPoint.json");
    }

    @Test
    public void testCreateDescription() throws Exception {
        // Create additional description for c2
        // C2 initially has one description, so it should have two afterwards
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2"))
                .entity(descriptionTestStr).post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));

        // Check ID is the correct concatenation of all the scope IDs...
        JsonNode idValue = rootNode
                .path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals("nl-r1-c1-c2.en-another_description", idValue.textValue());

        // Check the identifier is present and correct...
        JsonNode identValue = rootNode
                .path(Bundle.DATA_KEY)
                .path(Ontology.IDENTIFIER_KEY);
        assertFalse(identValue.isMissingNode());
        assertEquals(TEST_DESCRIPTION_IDENTIFIER, identValue.textValue());
    }

    @Test
    public void testUpdateDescription() throws Exception {
        // Update description for c2
        // C2 initially has one description, and should still have one afterwards
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "cd2"))
                .entity(descriptionTestStr).put(ClientResponse.class);
        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode idValue = rootNode
                .path(Bundle.DATA_KEY)
                .path(Ontology.IDENTIFIER_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(TEST_DESCRIPTION_IDENTIFIER, idValue.textValue());
        // Assert there are no extra descriptions
        assertTrue(rootNode.path(Bundle.REL_KEY).path(
                Ontology.DESCRIPTION_FOR_ENTITY).path(1).isMissingNode());
    }

    @Test
    public void testDeleteDescription() throws Exception {
        // Delete description for c2
        // C2 initially has one description, so there should be none afterwards
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "cd2"))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateDeleteAccessPoints() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "cd2", Entities.ACCESS_POINT))
                .entity(accessPointTestStr).post(ClientResponse.class);
        assertStatus(CREATED, response);
        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode idNode = rootNode
                .path(Bundle.ID_KEY);
        assertFalse(idNode.isMissingNode());
        String value = idNode.asText();

        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "cd2",
                        Entities.ACCESS_POINT, value))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }
}
