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
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.NO_CONTENT;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.GenericResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DescriptionResourceClientTest extends AbstractResourceClientTest {

    private String accessPointTestStr;

    @Before
    public void setUp() throws Exception {
        accessPointTestStr = readResourceFileAsString("AccessPoint.json");
    }

    @Test
    public void testCreateDeleteAccessPoints() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "descriptions", "cd2", "access-points"))
                .entity(accessPointTestStr).post(ClientResponse.class);
        assertStatus(CREATED, response);
        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode idNode = rootNode
                .path(Bundle.ID_KEY);
        assertFalse(idNode.isMissingNode());
        String value = idNode.asText();

        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(ENDPOINT, "c2", "descriptions", "cd2",
                        "access-points", value))
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);
    }
}
