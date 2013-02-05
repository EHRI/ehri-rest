package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Bundle;

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
                + "/userProfile");
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
        queryParams.add("key", AccessibleEntity.IDENTIFIER_KEY);
        queryParams.add("value", FETCH_NAME);

        WebResource resource = client.resource(
                getExtensionEntryPointUri() + "/userProfile").queryParams(
                queryParams);

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
                + "/userProfile");
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
                .resource(getExtensionEntryPointUri() + "/userProfile");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(entityBundle.toString()).put(ClientResponse.class);
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
    public void testUpdateWithIntegrityError() throws Exception {

        // -create data for testing
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/userProfile");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Create a bundle with a username that's already been taken.
        Bundle bundle = Bundle.fromString(response.getEntity(String.class))
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY, "mike");

        // Get created doc via the response location?
        URI location = response.getLocation();

        response = client
                .resource(location)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(bundle.toString())
                .put(ClientResponse.class);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }
    
    @Test
    public void testCreateDeleteUserProfileWithGroups() throws Exception {
    	final String GROUP_ID1 = "dans";
    	final String GROUP_ID2 = "kcl";
        
    	// Create
    	URI uri = UriBuilder.fromPath(getExtensionEntryPointUri()
                + "/userProfile")
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
    	URI uri = UriBuilder.fromPath(getExtensionEntryPointUri()
                + "/userProfile")
                .queryParam("group", GROUP_ID_EXISTING)
                .queryParam("group", GROUP_ID_NONEXISTING)
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
}
