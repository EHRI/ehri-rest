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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;

public class UserProfileResourceClientTest extends AbstractResourceClientTest {

    private static final String FETCH_NAME = "mike";
    private static final String UPDATED_NAME = "UpdatedNameTEST";

    private static final String jsonUserProfileTestString = "{\"type\":\"UserProfile\", \"data\":{\"identifier\": " +
            "\"test-user\", \"name\":\"testUserName1\"}}";

    @Test
    public void testCreateDeleteUserProfile() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.USER_PROFILE))
                .post(Entity.json(jsonUserProfileTestString), Response.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);

        // Get created entity via the response location?
        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(Response.class);
        assertStatus(OK, response);
        assertValidJsonData(response);
    }

    @Test
    public void testGetByKeyValue() throws Exception {
        WebTarget resource = client.target(entityUri(Entities.USER_PROFILE))
                .queryParam("key", Ontology.IDENTIFIER_KEY)
                .queryParam("value", FETCH_NAME);

        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(Entity.json(jsonUserProfileTestString), Response.class);

        assertStatus(CREATED, response);
    }

    @Test
    public void testUpdateUserProfile() throws Exception {

        // -create data for testing
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.USER_PROFILE))
                .post(Entity.json(jsonUserProfileTestString), Response.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(Response.class);
        assertStatus(OK, response);

        // -get the data and change it
        String json = response.readEntity(String.class);
        Bundle entityBundle = Bundle.fromString(json).withDataValue("name",
                UPDATED_NAME);

        // -update
        response = jsonCallAs(getAdminUserProfileId(), location)
                .put(Entity.json(entityBundle.toJson()), Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.readEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testCreateUserProfileWithIntegrityErrir() throws Exception {
        // Create
        URI uri = entityUri(Entities.USER_PROFILE);
        Response response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(jsonUserProfileTestString), Response.class);
        assertStatus(CREATED, response);

        // Doing exactly the same thing twice should result in a
        // ValidationError because the same IDs will be generated...
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(jsonUserProfileTestString), Response.class);
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testCreateDeleteUserProfileWithGroups() throws Exception {
        final String GROUP_ID1 = "dans";
        final String GROUP_ID2 = "kcl";

        // Create
        URI uri = entityUriBuilder(Entities.USER_PROFILE)
                .queryParam("group", GROUP_ID1)
                .queryParam("group", GROUP_ID2)
                .build();

        Response response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(jsonUserProfileTestString), Response.class);

        assertStatus(CREATED, response);

        // Get created entity via the response location?
        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(Response.class);
        assertStatus(OK, response);

        // check that the groups are there
        Set<String> groupIds = getGroupIdsFromEntityJson(response.readEntity(String.class));
        assertTrue(groupIds.contains(GROUP_ID1));
        assertTrue(groupIds.contains(GROUP_ID2));
    }

    private Set<String> getGroupIdsFromEntityJson(String jsonString) throws IOException {
        Set<String> groupIds = Sets.newHashSet();
        JsonNode node = jsonMapper.readTree(jsonString);
        JsonNode belongs = node.path(Bundle.REL_KEY).path(Ontology.ACCESSOR_BELONGS_TO_GROUP);
        int i = 0;
        while (belongs.has(i)) {
            groupIds.add(belongs.path(i).path(Bundle.ID_KEY).asText());
            i++;
        }
        return groupIds;
    }

    @Test
    public void testCreateUserProfileWithNonexistingGroup() throws Exception {
        final String GROUP_ID_EXISTING = "dans";
        final String GROUP_ID_NONEXISTING = "non-existing-e6d86e97-5eb1-4030-9d21-3eabea6f57ca";

        // Create
        URI uri = entityUriBuilder(Entities.USER_PROFILE)
                .queryParam(AbstractResource.GROUP_PARAM, GROUP_ID_EXISTING)
                .queryParam(AbstractResource.GROUP_PARAM, GROUP_ID_NONEXISTING)
                .build();

        Response response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(jsonUserProfileTestString), Response.class);

        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testFollowAndUnfollow() throws Exception {
        String user1 = "reto";
        String user2 = "mike";
        String user3 = "linda";

        URI followingUrl = entityUri(Entities.USER_PROFILE, user1, "following");
        List<Bundle> followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());

        URI followUrl1 = entityUriBuilder(Entities.USER_PROFILE, user1, "following")
                .queryParam(AbstractResource.ID_PARAM, user2).build();
        Response response = jsonCallAs(user1, followUrl1).post(Entity.json(""), Response.class);
        assertStatus(NO_CONTENT, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(1, followers.size());

        URI followUrl2 = entityUriBuilder(Entities.USER_PROFILE, user1, "following")
                .queryParam(AbstractResource.ID_PARAM, user3).build();
        response = jsonCallAs(user1, followUrl2).post(Entity.json(""), Response.class);
        assertStatus(NO_CONTENT, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(2, followers.size());

        // Hitting the same URL as a GET should give us a boolean...
        URI isFollowingUrl = entityUri(Entities.USER_PROFILE, user1, "is-following", user2);
        response = jsonCallAs(user1, isFollowingUrl).get(Response.class);
        assertStatus(OK, response);
        assertEquals("true", response.readEntity(String.class));

        URI hasFollowerUrl = entityUri(Entities.USER_PROFILE, user2, "is-follower", user1);
        response = jsonCallAs(user2, hasFollowerUrl).get(Response.class);
        assertStatus(OK, response);
        assertEquals("true", response.readEntity(String.class));

        response = jsonCallAs(user1, followUrl1).delete(Response.class);
        assertStatus(NO_CONTENT, response);
        followers = getItemList(followingUrl, user1);
        assertEquals(1, followers.size());
    }

    @Test
    public void testBlockAndUnblock() throws Exception {
        String user1 = getRegularUserProfileId();
        String user2 = getAdminUserProfileId();
        URI blockedUrl = entityUri(Entities.USER_PROFILE, user1, "blocked");
        List<Bundle> blocked = getItemList(blockedUrl, user1);
        assertTrue(blocked.isEmpty());

        URI blockUrl = entityUriBuilder(Entities.USER_PROFILE, user1, "blocked")
                .queryParam(AbstractResource.ID_PARAM, user2).build();
        Response response = jsonCallAs(user1, blockUrl)
                .post(Entity.json(""), Response.class);
        assertStatus(NO_CONTENT, response);
        blocked = getItemList(blockedUrl, user1);
        assertFalse(blocked.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isBlockingUrl = entityUri(Entities.USER_PROFILE, user1, "is-blocking", user2);
        response = jsonCallAs(user1, isBlockingUrl).get(Response.class);
        assertStatus(OK, response);
        assertEquals("true", response.readEntity(String.class));

        response = jsonCallAs(user1, blockUrl).delete(Response.class);
        assertStatus(NO_CONTENT, response);
        blocked = getItemList(blockedUrl, user1);
        assertTrue(blocked.isEmpty());
    }

    @Test
    public void testWatchingAndUnwatching() throws Exception {
        String user1 = getAdminUserProfileId();
        String item1 = "c1";
        String item2 = "c2";
        URI watchersUrl = entityUri(Entities.USER_PROFILE, user1, "watching");
        List<Bundle> watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());

        URI watchUrl1 = entityUriBuilder(Entities.USER_PROFILE, user1, "watching")
                .queryParam(AbstractResource.ID_PARAM, item1).build();
        Response response = jsonCallAs(user1, watchUrl1).post(Entity.json(""), Response.class);
        assertStatus(NO_CONTENT, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(1, watching.size());

        URI watchUrl2 = entityUriBuilder(Entities.USER_PROFILE, user1, "watching")
                .queryParam(AbstractResource.ID_PARAM, item2).build();
        response = jsonCallAs(user1, watchUrl2).post(Entity.json(""), Response.class);
        assertStatus(NO_CONTENT, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(2, watching.size());

        // Hitting the same URL as a GET should give us a boolean...
        URI isWatchingUrl = entityUri(Entities.USER_PROFILE, user1, "is-watching", item1);
        response = jsonCallAs(user1, isWatchingUrl).get(Response.class);
        assertStatus(OK, response);
        assertEquals("true", response.readEntity(String.class));

        response = jsonCallAs(user1, watchUrl1).delete(Response.class);
        assertStatus(NO_CONTENT, response);
        watching = getItemList(watchersUrl, user1);
        assertEquals(1, watching.size());
    }
}
