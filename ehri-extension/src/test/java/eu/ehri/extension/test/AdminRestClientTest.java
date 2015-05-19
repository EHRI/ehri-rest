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
import eu.ehri.extension.AdminResource;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static eu.ehri.project.models.Group.ADMIN_GROUP_IDENTIFIER;
import static eu.ehri.extension.AbstractRestResource.AUTH_HEADER_NAME;
import static eu.ehri.extension.AdminResource.ENDPOINT;

/**
 * Test admin REST functions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AdminRestClientTest extends BaseRestClientTest {

    @Test
    public void testExportGraphSONAsAnon() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_exportGraphSON"));
        ClientResponse response = resource.get(ClientResponse.class);
        String data = response.getEntity(String.class);
        assertStatus(OK, response);
        assertTrue("Anon export must contain publicly-visible item ann1", data.contains("ann1"));
        assertFalse("Anon export must not contain restricted item ann3", data.contains("ann3"));
        assertTrue("Anon export must contain promoted item ann4", data.contains("ann4"));
    }

    @Test
    public void testExportGraphSONAsAdmin() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_exportGraphSON"));
        ClientResponse response = resource.header(AUTH_HEADER_NAME, ADMIN_GROUP_IDENTIFIER)
                .get(ClientResponse.class);
        String data = response.getEntity(String.class);
        assertStatus(OK, response);
        assertTrue("Admin export must contain publicly-visible item ann1", data.contains("ann1"));
        assertTrue("Admin export must contain restricted item ann3", data.contains("ann3"));
    }

    @Test
    public void testReindexInternal() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "_reindexInternal"));
        ClientResponse response = resource.post(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateDefaultUser() throws Exception {
        // Create
        WebResource resource = client.resource(ehriUri(ENDPOINT, "createDefaultUserProfile"));
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertStatus(CREATED, response);
        String json = response.getEntity(String.class);
        Bundle bundle = Bundle.fromString(json);
        String ident = (String) bundle.getData().get(Ontology.IDENTIFIER_KEY);
        assertTrue(ident != null);
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

        // Create another user and ensure their idents are different and
        // incremental
        WebResource resource2 = client.resource(ehriUri(ENDPOINT, "createDefaultUserProfile"));
        response = resource2.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertStatus(CREATED, response);
        String json2 = response.getEntity(String.class);
        Bundle bundle2 = Bundle.fromString(json2);
        String ident2 = (String) bundle2.getData().get(
                Ontology.IDENTIFIER_KEY);
        assertEquals(parseUserId(ident) + 1L, parseUserId(ident2));
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

    }

    // Helpers
    private long parseUserId(String ident) {
        return Long.parseLong(ident.replace(
                AdminResource.DEFAULT_USER_ID_PREFIX, ""));
    }
}
