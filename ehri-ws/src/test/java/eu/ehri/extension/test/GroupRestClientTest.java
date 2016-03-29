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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.base.AbstractRestResource;
import eu.ehri.extension.GroupResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.NO_CONTENT;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroupRestClientTest extends AbstractRestClientTest {

    static final String TEST_GROUP_NAME = "admin";
    static final String CURRENT_ADMIN_USER = "mike";
    static final String NON_ADMIN_USER = "reto";

    @Test
    public void testCreateGroup() throws Exception {
        // Create
        String jsonGroupTestString = "{\"type\": \"Group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.GROUP)).entity(jsonGroupTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);
        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateGroupWithMembers() throws Exception {
        // Create
        String jsonGroupTestString = "{\"type\": \"Group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";
        URI uri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(AbstractRestResource.RESOURCE_ENDPOINT_PREFIX)
                .segment(Entities.GROUP)
                .queryParam(GroupResource.MEMBER_PARAM, "linda").build();
        ClientResponse response = client.resource(uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .entity(jsonGroupTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);
        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(),
                UriBuilder.fromUri(location).segment("list").build())
                .get(ClientResponse.class);

        assertStatus(OK, response);

        // check results
        Set<String> ids = getIdsFromEntityListJson(response.getEntity(String.class));
        assertTrue(ids.contains("linda"));
        assertEquals(1, ids.size());
    }

    @Test
    public void testAddUser() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.GROUP, TEST_GROUP_NAME, NON_ADMIN_USER))
                .post(ClientResponse.class);
        assertStatus(NO_CONTENT, response);
    }

    @Test
    public void testRemoveUser() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.GROUP, TEST_GROUP_NAME, CURRENT_ADMIN_USER))
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);
    }

    @Test
    public void testListGroupMembers() throws Exception {
        final String GROUP_ID = "kcl";

        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.GROUP, GROUP_ID, "list"))
                .get(ClientResponse.class);

        assertStatus(OK, response);

        // check results
        //System.out.println(response.getEntity(String.class));
        Set<String> ids = getIdsFromEntityListJson(response.getEntity(String.class));
        // for 'kcl' it should be 'mike', 'reto' and nothing else
        assertTrue(ids.contains("mike"));
        assertTrue(ids.contains("reto"));
        assertEquals(2, ids.size());
    }

    /**
     * Helpers **
     */

    private Set<String> getIdsFromEntityListJson(String jsonString) throws IOException {
        TypeReference<ArrayList<HashMap<String, Object>>> typeReference = new TypeReference<ArrayList<HashMap<String,
                Object>>>() {
        };
        List<HashMap<String, Object>> data = jsonMapper.readValue(jsonString, typeReference);
        Set<String> ids = Sets.newHashSet();
        for (Map<String, Object> bundle : data) {
            ids.add((String) bundle.get(Bundle.ID_KEY));
        }
        return ids;
    }
}
