package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static eu.ehri.extension.UserProfileResource.*;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.persistence.Bundle;

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
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.USER_PROFILE);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get created entity via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
    }

    @Test
    public void testGetByKeyValue() throws Exception {
        // -create data for testing
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("key", Ontology.IDENTIFIER_KEY);
        queryParams.add("value", FETCH_NAME);

        WebResource resource = client.resource(
                getExtensionEntryPointUri() + "/" + Entities.USER_PROFILE)
                .queryParams(queryParams);

        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testUpdateUserProfile() throws Exception {

        // -create data for testing
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.USER_PROFILE);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and change it
        String json = response.getEntity(String.class);
        Bundle entityBundle = Bundle.fromString(json).withDataValue("name",
                UPDATED_NAME);

        // -update
        resource = client
                .resource(getExtensionEntryPointUri() + "/" + Entities.USER_PROFILE);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(entityBundle.toJson()).put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testCreateUserProfileWithIntegrityErrir() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.USER_PROFILE);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Doing exactly the same thing twice should result in a
        // ValidationError because the same IDs will be generated...
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);
        System.out.println(response.getEntity(String.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
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
    	
        WebResource resource = client.resource(uri);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get created entity via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                //.type(MediaType.APPLICATION_JSON_TYPE)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        // check that the groups are there
        Set<String> groupIds = getGroupIdsFromEntityJson(response.getEntity(String.class));
        assertTrue(groupIds.contains(GROUP_ID1));
        assertTrue(groupIds.contains(GROUP_ID2));
    }
    
    private Set<String> getGroupIdsFromEntityJson(String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        //System.out.println("id: " + obj.get("id"));
        Set<String> groupIds = new HashSet<String>();
        JSONArray jsonArray = ((JSONObject) obj.get("relationships")).getJSONArray("belongsTo");
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            //System.out.println("id: " + item.get("id"));
            groupIds.add(item.getString("id").toString());
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
    	
        WebResource resource = client.resource(uri);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testFollowAndUnfollow() throws Exception {
        String user1 = getRegularUserProfileId();
        String user2 = getAdminUserProfileId();
        String followingUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + FOLLOWING;
        List<Map<String,Object>> followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());

        URI followUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(FOLLOW)
                .segment(user2)
                .build();
        ClientResponse response = client.resource(followUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        followers = getItemList(followingUrl, user1);
        assertFalse(followers.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isFollowingUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(IS_FOLLOWING)
                .segment(user2)
                .build();
        response = client.resource(isFollowingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("true", response.getEntity(String.class));

        URI hasFollowerUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user2)
                .segment(IS_FOLLOWER)
                .segment(user1)
                .build();
        response = client.resource(hasFollowerUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user2)
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("true", response.getEntity(String.class));

        response = client.resource(followUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        followers = getItemList(followingUrl, user1);
        assertTrue(followers.isEmpty());
    }

    @Test
    public void testWatchingAndUnwatching() throws Exception {
        String user1 = getAdminUserProfileId();
        String item = "c1";
        String watchersUrl = "/" + Entities.USER_PROFILE + "/" + user1 + "/" + WATCHING;
        List<Map<String,Object>> watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());

        URI watchUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(WATCH)
                .segment(item)
                .build();
        ClientResponse response = client.resource(watchUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        watching = getItemList(watchersUrl, user1);
        assertFalse(watching.isEmpty());

        // Hitting the same URL as a GET should give us a boolean...
        URI isWatchingUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user1)
                .segment(IS_WATCHING)
                .segment(item)
                .build();
        response = client.resource(isWatchingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("true", response.getEntity(String.class));

        response = client.resource(watchUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user1)
                .delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        watching = getItemList(watchersUrl, user1);
        assertTrue(watching.isEmpty());
    }

}
