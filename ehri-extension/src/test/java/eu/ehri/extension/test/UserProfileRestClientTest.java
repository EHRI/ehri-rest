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
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static eu.ehri.extension.UserProfileResource.*;
import static org.junit.Assert.*;

public class UserProfileRestClientTest extends BaseRestClientTest {

    static final String FETCH_NAME = "mike";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonUserProfileTestString = "{\"type\":\"userProfile\", \"data\":{\"identifier\": \"test-user\", \"name\":\"testUserName1\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(UserProfileRestClientTest.class.getName());
    }

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

        // Get created entity via the response location?
        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);
        // TODO again test json
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
        // TODO test if json is valid?
        // response.getEntity(String.class)

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
    public void testFollowAndUnfollow() throws Exception {
        String user1 = getRegularUserProfileId();
        String user2 = getAdminUserProfileId();
        String followingUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + FOLLOWING;
        List<Map<String, Object>> followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());

        URI followUrl = ehriUri(Entities.USER_PROFILE, user1, FOLLOW, user2);
        ClientResponse response = jsonCallAs(user1, followUrl).post(ClientResponse.class);
        assertStatus(OK, response);
        followers = getItemList(followingUrl, user1);
        assertFalse(followers.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isFollowingUrl = ehriUri(Entities.USER_PROFILE, user1, IS_FOLLOWING, user2);
        response = jsonCallAs(user1, isFollowingUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        URI hasFollowerUrl = ehriUri(Entities.USER_PROFILE, user2, IS_FOLLOWER, user1);
        response = jsonCallAs(user2, hasFollowerUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        response = jsonCallAs(user1, followUrl).delete(ClientResponse.class);
        assertStatus(OK, response);
        followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());
    }

    @Test
    public void testWatchingAndUnwatching() throws Exception {
        String user1 = getAdminUserProfileId();
        String item = "c1";
        String watchersUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + WATCHING;
        List<Map<String, Object>> watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());

        URI watchUrl = ehriUri(Entities.USER_PROFILE, user1, WATCH, item);
        ClientResponse response = jsonCallAs(user1, watchUrl).post(ClientResponse.class);
        assertStatus(OK, response);
        watching = getItemList(watchersUrl, user1);
        assertFalse(watching.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isWatchingUrl = ehriUri(Entities.USER_PROFILE, user1, IS_WATCHING, item);
        response = jsonCallAs(user1, isWatchingUrl).get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("true", response.getEntity(String.class));

        response = jsonCallAs(user1, watchUrl).delete(ClientResponse.class);
        assertStatus(OK, response);
        watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());
    }
}
