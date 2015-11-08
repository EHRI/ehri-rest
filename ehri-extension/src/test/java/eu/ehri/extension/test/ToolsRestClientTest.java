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

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.ToolsResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test admin REST functions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ToolsRestClientTest extends BaseRestClientTest {
    @Test
    public void testPropertyRename() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_findReplacePropertyValue"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("name", "streetAddress")
                .queryParam("from", "Strand")
                .queryParam("to", "Drury Lane");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("1", response.getEntity(String.class));
    }

    @Test
    public void testPropertyRenameRE() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_findReplacePropertyValueRE"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("name", "webpage")
                .queryParam("pattern", "^http:")
                .queryParam("replace", "https:");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("3", response.getEntity(String.class));
    }

    @Test
    public void testPropertyKeyRename() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_findReplacePropertyName"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("from", "streetAddress")
                .queryParam("to", "somethingElse");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("2", response.getEntity(String.class));
    }

    @Test
    public void testRegenerateId() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_regenerateId", "c1"))
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals(1, out.split("\r\n|\r|\n").length);
        assertTrue(out.contains("nl-r1-c1"));
    }

    @Test
    public void testRegenerateIdsForScope() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_regenerateIdsForScope", "r1"))
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
        WebResource resource = client.resource(ehriUri(ENDPOINT,
                "_regenerateIdsForType", "documentaryUnit"))
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
    public void testRegenerateDescriptionIds() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_regenerateDescriptionIds"))
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
}
