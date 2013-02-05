package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;

public class GroupRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";
    static final String TEST_GROUP_NAME = "admin";
    static final String CURRENT_ADMIN_USER = "mike";
    static final String NON_ADMIN_USER = "reto";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(GroupRestClientTest.class.getName());
    }

    @Test
    public void testCreateDeleteGroup() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/group");
        String jsonGroupTestString = "{\"type\": \"group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonGroupTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        // Get created doc via the response location?
        URI location = response.getLocation();
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testAddUser() throws Exception {
        // Create
        WebResource resource = client.resource(
                UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.GROUP).segment(TEST_GROUP_NAME)
                .segment(NON_ADMIN_USER).build());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testRemoveUser() throws Exception {
        // Create
        WebResource resource = client.resource(
                UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.GROUP).segment(TEST_GROUP_NAME)
                .segment(CURRENT_ADMIN_USER).build());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .delete(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
    }    
    
    @Test
    public void testListGroupMembers() throws Exception {
    	final String GROUP_ID = "kcl";
    	 WebResource resource = client.resource(getExtensionEntryPointUri()
                 + "/group/" + GROUP_ID + "/list");
    	 
    	ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        
    	assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    	
    	// test results
    	//System.out.println(response.getEntity(String.class));
    	
    	Set<String> ids = getIdsFromEntityListJson(response.getEntity(String.class));
    	// for 'kcl' it should be 'mike', 'reto' and nothing else
    	assertTrue(ids.contains("mike"));
    	assertTrue(ids.contains("reto"));
    	assertEquals(2, ids.size());
    }  
    
    /*** helpers ***/
    
    private Set<String> getIdsFromEntityListJson(String jsonString) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonString);
        //System.out.println("id: " + obj.get("id"));
        Set<String> ids = new HashSet<String>();
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            //System.out.println("id: " + item.get("id"));
            ids.add(item.getString("id").toString());
        }
        return ids;
    }
}
