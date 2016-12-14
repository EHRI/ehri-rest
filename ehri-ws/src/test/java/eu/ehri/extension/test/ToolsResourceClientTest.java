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
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.ToolsResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test web service miscellaneous functions.
 */
public class ToolsResourceClientTest extends AbstractResourceClientTest {
    @Test
    public void testRegenerateIds() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids"))
                .queryParam("id", "c1")
                .queryParam("id", "c4")
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals(2, out.split("\r\n|\r|\n").length);
        assertTrue(out.contains("nl-r1-c1"));
        assertTrue(out.contains("nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForScope() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids-for-scope", "r1"))
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals(4, out.split("\r\n|\r|\n").length);
        assertTrue(out.contains("nl-r1-c1"));
        assertTrue(out.contains("nl-r1-c1-c2"));
        assertTrue(out.contains("nl-r1-c1-c2-c3"));
        assertTrue(out.contains("nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForType() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids-for-type",
                Entities.DOCUMENTARY_UNIT)).queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals(4, out.split("\r\n|\r|\n").length);
        assertTrue(out.contains("nl-r1-c1"));
        assertTrue(out.contains("nl-r1-c1-c2"));
        assertTrue(out.contains("nl-r1-c1-c2-c3"));
        assertTrue(out.contains("nl-r1-c4"));
    }

    @Test
    public void testRegenerateDescriptionIds() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-description-ids"))
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        // 2 Historical agent descriptions
        // 4 Repository descriptions
        // 6 Doc Unit descriptions (total 7, 1 is okay in fixtures)
        // 1 Concept description
        assertEquals("13", out);
    }
    
    @Test
    public void testRelinking() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "relink-targets"));
        String data = "from,to,label\na1,a2,Test\n";
        ClientResponse response = resource.entity(data).post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals("a1,a2,2\n", out);
    }
}
