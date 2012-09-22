package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.EntityBundle;

public class UserProfileRestClientTest  extends BaseRestClientTest {

	static final String UPDATED_NAME = "UpdatedNameTEST";

	private String jsonUserProfileTestString = "{\"data\":{\"userId\":-1,\"name\":\"testUserName1\",\"isA\":\"userProfile\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    	initializeTestDb(UserProfileRestClientTest.class.getName());
    }
    
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * 	 CR(U)D cycle
	 * 
	 */
	@Test
	public void testCreateDeleteUserProfile() throws Exception {		
		// Create
		WebResource resource = client.resource(extensionEntryPointUri
				+ "/userProfile");
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonUserProfileTestString).post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		
		// Get created entity via the response location?
		URI location = response.getLocation();
		
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO again test json

		delete(location);
	}
	
	@Test
	public void testUpdateUserProfile() throws Exception {		
		
		// -create data for testing
		WebResource resource = client.resource(extensionEntryPointUri + "/userProfile");
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonUserProfileTestString)
				.post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO test if json is valid?
		//response.getEntity(String.class)
		
		// Get created doc via the response location?
		URI location = response.getLocation();

		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// -get the data and change it
		String json = response.getEntity(String.class);
		EntityBundle<UserProfile> entityBundle = converter.jsonToBundle(json); 
		Map<String, Object> data = entityBundle.getData();
		data.put("name", UPDATED_NAME);
		entityBundle = entityBundle.setData(data);
		String toUpdateJson = converter.bundleToJson(entityBundle);
		
		// -update
		resource = client.resource(extensionEntryPointUri + "/userProfile");
		response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(toUpdateJson)
				.put(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
				
		// -get the data and convert to a bundle, is it changed?
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// -get the data and convert to a bundle, is it OK?
		String updatedJson = response.getEntity(String.class);
		EntityBundle<DocumentaryUnit> updatedEntityBundle = converter.jsonToBundle(updatedJson); 
		Map<String, Object> updatedData = updatedEntityBundle.getData();
		assertEquals(UPDATED_NAME, updatedData.get("name"));
		
		delete(location);
	}
	
}
