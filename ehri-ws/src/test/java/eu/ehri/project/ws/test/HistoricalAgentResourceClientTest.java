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

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;

public class HistoricalAgentResourceClientTest extends AbstractResourceClientTest {

    static final String TEST_ID = "a1";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String historicalAgentTestData;

    @Before
    public void setUp() throws Exception {
        historicalAgentTestData = readResourceFileAsString("HistoricalAgent.json");
    }

    @Test
    public void testCreateHistoricalAgent() {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.AUTHORITATIVE_SET, "auths"))
                .entity(historicalAgentTestData)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), location).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateHistoricalAgentWithExistingIdentifier() throws Exception {
        String json = Bundle.fromString(historicalAgentTestData)
                .withDataValue(Ontology.IDENTIFIER_KEY, "a2").toJson();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.AUTHORITATIVE_SET, "auths"))
                .entity(json)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        ClientResponse response2 = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.AUTHORITATIVE_SET, "auths"))
                .entity(json)
                .post(ClientResponse.class);
        assertStatus(BAD_REQUEST, response2);
    }

    @Test
    public void testUpdateHistoricalAgentByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.AUTHORITATIVE_SET, "auths")).entity(historicalAgentTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Obtain some update data.
        String updateData = Bundle.fromString(historicalAgentTestData)
                .withDataValue("name", UPDATED_NAME).toJson();

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .entity(updateData)
                .put(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateHistoricalAgentWithDeserializationError() {
        // Create
        String badHistoricalAgentTestData = "{\"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.AUTHORITATIVE_SET, "auths"))
                .entity(badHistoricalAgentTestData)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testDeleteHistoricalAgent() {
        URI uri = entityUri(Entities.HISTORICAL_AGENT, TEST_ID);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(GONE, response);
    }
}
