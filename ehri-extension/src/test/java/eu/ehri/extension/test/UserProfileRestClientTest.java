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
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.UserProfileResource.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserProfileRestClientTest extends BaseRestClientTest {

    static final String FETCH_NAME = "mike";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonUserProfileTestString = "{\"type\":\"userProfile\", \"data\":{\"identifier\": \"test-user\", \"name\":\"testUserName1\"}}";

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteUserProfile() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.USER_PROFILE))
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);

        // Get created entity via the response location?
        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);
        assertValidJsonData(response);
    }

    @Test
    public void testGetByKeyValue() throws Exception {
        // -create data for testing
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("key", Ontology.IDENTIFIER_KEY);
        queryParams.add("value", FETCH_NAME);

        WebResource resource = client.resource(ehriUri(Entities.USER_PROFILE))
                .queryParams(queryParams);

        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertStatus(CREATED, response);
    }

    @Test
    public void testUpdateUserProfile() throws Exception {

        // -create data for testing
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.USER_PROFILE))
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and change it
        String json = response.getEntity(String.class);
        Bundle entityBundle = Bundle.fromString(json).withDataValue("name",
                UPDATED_NAME);

        // -update
        response = jsonCallAs(getAdminUserProfileId(), ehriUri(Entities.USER_PROFILE))
                .entity(entityBundle.toJson()).put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testCreateUserProfileWithIntegrityErrir() throws Exception {
        // Create
        URI uri = ehriUri(Entities.USER_PROFILE);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(jsonUserProfileTestString).post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Doing exactly the same thing twice should result in a
        // ValidationError because the same IDs will be generated...
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(jsonUserProfileTestString).post(ClientResponse.class);
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testCreateDeleteUserProfileWithGroups() throws Exception {
        final String GROUP_ID1 = "dans";
        final String GROUP_ID2 = "kcl";

        // Create
        URI uri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .queryParam("group", GROUP_ID1)
                .queryParam("group", GROUP_ID2)
                .build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(jsonUserProfileTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created entity via the response location?
        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // check that the groups are there
        Set<String> groupIds = getGroupIdsFromEntityJson(response.getEntity(String.class));
        assertTrue(groupIds.contains(GROUP_ID1));
        assertTrue(groupIds.contains(GROUP_ID2));
    }

    private Set<String> getGroupIdsFromEntityJson(String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        Set<String> groupIds = new HashSet<String>();
        JSONArray jsonArray = ((JSONObject) obj.get("relationships")).getJSONArray("belongsTo");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            groupIds.add(item.getString("id"));
        }
        return groupIds;
    }

    @Test
    public void testCreateUserProfileWithNonexistingGroup() throws Exception {
        final String GROUP_ID_EXISTING = "dans";
        final String GROUP_ID_NONEXISTING = "non-existing-e6d86e97-5eb1-4030-9d21-3eabea6f57ca";

        // Create
        URI uri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .queryParam(AbstractRestResource.GROUP_PARAM, GROUP_ID_EXISTING)
                .queryParam(AbstractRestResource.GROUP_PARAM, GROUP_ID_NONEXISTING)
                .build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(jsonUserProfileTestString)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
    }

    @Test
    @Ignore
    public void testFollowAndUnfollow() throws Exception {
        String user1 = "reto";
        String user2 = "mike";
        String user3 = "linda";

        String followingUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + FOLLOWING;
        List<Map<String, Object>> followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());

        URI followUrl1 = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(FOLLOWING)
                .queryParam(ID_PARAM, user2).build();
        ClientResponse response = jsonCallAs(user1, followUrl1).post(ClientResponse.class);
        assertStatus(OK, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(1, followers.size());

        URI followUrl2 = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(FOLLOWING)
                .queryParam(ID_PARAM, user3).build();
        response = jsonCallAs(user1, followUrl2).post(ClientResponse.class);
        assertStatus(OK, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(2, followers.size());

        // Hitting the same URL as a GET should give us a boolean...
        URI isFollowingUrl = ehriUri(Entities.USER_PROFILE, user1, IS_FOLLOWING, user2);
        response = jsonCallAs(user1, isFollowingUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        URI hasFollowerUrl = ehriUri(Entities.USER_PROFILE, user2, IS_FOLLOWER, user1);
        response = jsonCallAs(user2, hasFollowerUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        response = jsonCallAs(user1, followUrl1).delete(ClientResponse.class);
        assertStatus(OK, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(1, followers.size());
    }

    @Test
    public void testBlockAndUnblock() throws Exception {
        String user1 = getRegularUserProfileId();
        String user2 = getAdminUserProfileId();
        String blockedUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + BLOCKED;
        List<Map<String, Object>> blocked = getItemList(blockedUrl, user1);
        assertTrue(blocked.isEmpty());

        URI blockUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(BLOCKED)
                .queryParam(ID_PARAM, user2).build();
        ClientResponse response = jsonCallAs(user1, blockUrl)
                .post(ClientResponse.class);
        assertStatus(OK, response);
        blocked = getItemList(blockedUrl, user1);
        assertFalse(blocked.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isBlockingUrl = ehriUri(Entities.USER_PROFILE, user1, IS_BLOCKING, user2);
        response = jsonCallAs(user1, isBlockingUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        response = jsonCallAs(user1, blockUrl).delete(ClientResponse.class);
        assertStatus(OK, response);
        blocked = getItemList(blockedUrl, user1);
        assertTrue(blocked.isEmpty());
    }

    @Test
    public void testWatchingAndUnwatching() throws Exception {
        String user1 = getAdminUserProfileId();
        String item1 = "c1";
        String item2 = "c2";
        String watchersUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + WATCHING;
        List<Map<String, Object>> watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());

        URI watchUrl1 = UriBuilder.fromPath(getExtensionEntryPointUri())
            .segment(Entities.USER_PROFILE)
            .segment(user1)
            .segment(WATCHING)
            .queryParam(ID_PARAM, item1).build();
        ClientResponse response = jsonCallAs(user1, watchUrl1).post(ClientResponse.class);
        assertStatus(OK, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(1, watching.size());

        URI watchUrl2 = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(WATCHING)
                .queryParam(ID_PARAM, item2).build();
        response = jsonCallAs(user1, watchUrl2).post(ClientResponse.class);
        assertStatus(OK, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(2, watching.size());

        // Hitting the same URL as a GET should give us a boolean...
        URI isWatchingUrl = ehriUri(Entities.USER_PROFILE, user1, IS_WATCHING, item1);
        response = jsonCallAs(user1, isWatchingUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        response = jsonCallAs(user1, watchUrl1).delete(ClientResponse.class);
        assertStatus(OK, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(1, watching.size());
    }
}
