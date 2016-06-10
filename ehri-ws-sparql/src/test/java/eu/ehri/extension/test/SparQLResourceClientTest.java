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
import eu.ehri.extension.SparQLResource;
import org.junit.Test;

import java.net.URI;
import java.net.URLEncoder;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test for the SparQL endpoint
 */
public class SparQLResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testSparQLQuery() throws Exception {
        String testQuery = readResourceFileAsString("testquery.sparql");
        String encoded = URLEncoder.encode(testQuery, "UTF-8").replaceAll("\\+", "%20");
        URI queryUri = ehriUriBuilder(SparQLResource.ENDPOINT)
                .queryParam(SparQLResource.QUERY_PARAM, encoded)
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri).get(ClientResponse.class);
        assertStatus(OK, response);
        String data = response.getEntity(String.class);
        JsonNode rootNode = jsonMapper.readTree(data);
        assertTrue(rootNode.isArray());
        JsonNode firstObject = rootNode.path(0);
        assertFalse(firstObject.isMissingNode());
        // NB: This relies on the ORDER by clause and linda being
        // the top name in ascending order - will break if someone
        // changes the fixtures.
        assertEquals("linda", firstObject.path("id").asText());
    }
}
