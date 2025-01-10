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

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.ws.AdminResource;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static eu.ehri.project.models.Group.ADMIN_GROUP_IDENTIFIER;
import static eu.ehri.project.ws.AdminResource.ENDPOINT;
import static eu.ehri.project.ws.base.AbstractResource.AUTH_HEADER_NAME;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;

/**
 * Test admin web service functions.
 */
public class AdminResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testAdminGetUserProfile() throws Exception {
        // get the admin user profile
        WebTarget resource = client.target(
                entityUri(Entities.USER_PROFILE, getAdminUserProfileId()));
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testExportGraphSONAsAnon() throws Exception {
        WebTarget resource = client.target(ehriUri(ENDPOINT, "export-graphson"));
        Response response = resource.request().get(Response.class);
        String data = response.readEntity(String.class);
        assertStatus(OK, response);
        assertTrue("Anon export must contain publicly-visible item ann1", data.contains("ann1"));
        assertFalse("Anon export must not contain restricted item ann3", data.contains("ann3"));
        assertTrue("Anon export must contain promoted item ann4", data.contains("ann4"));
    }

    @Test
    public void testExportGraphSONAsAdmin() throws Exception {
        WebTarget resource = client.target(ehriUri(ENDPOINT, "export-graphson"));
        Response response = resource.request().header(AUTH_HEADER_NAME, ADMIN_GROUP_IDENTIFIER)
                .get(Response.class);
        String data = response.readEntity(String.class);
        assertStatus(OK, response);
        assertTrue("Admin export must contain publicly-visible item ann1", data.contains("ann1"));
        assertTrue("Admin export must contain restricted item ann3", data.contains("ann3"));
    }

    @Test
    public void testExportJSON() throws Exception {
        WebTarget resource = client.target(ehriUri(ENDPOINT, "export-json"));
        Response response = resource
                .request()
                .header(AUTH_HEADER_NAME, ADMIN_GROUP_IDENTIFIER)
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateDefaultUser() throws Exception {
        // Create
        WebTarget resource = client.target(ehriUri(ENDPOINT, "create-default-user-profile"));
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""), Response.class);

        assertStatus(CREATED, response);
        Bundle bundle = response.readEntity(Bundle.class);
        String ident = (String) bundle.getData().get(Ontology.IDENTIFIER_KEY);
        assertNotNull(ident);
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

        // Create another user and ensure their idents are different and
        // incremental
        WebTarget resource2 = client.target(ehriUri(ENDPOINT, "create-default-user-profile"));
        response = resource2
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(""), Response.class);

        assertStatus(CREATED, response);
        Bundle bundle2 = response.readEntity(Bundle.class);
        String ident2 = (String) bundle2.getData().get(
                Ontology.IDENTIFIER_KEY);
        assertEquals(parseUserId(ident) + 1, parseUserId(ident2));
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));
    }

    // Helpers
    private int parseUserId(String ident) {
        return Integer.parseInt(ident.replace(
                AdminResource.DEFAULT_USER_ID_PREFIX, ""));
    }
}
