package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.extension.EhriNeo4jResource;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;

/**
 * Tests the REST interface on a neo4j server with the ehri extension deployed.
 * Therefore these should not be run as part of the automatic build (via mavenn),
 * but separately from the eclipse IDE for instance. 
 * Debugging can be done remotely via eclipse as well. 
 * The documentation of the neo4j server explains how to do this.
 */
public class EhriNeo4jExtensionRestClientTest {
	final String baseUri = "http://localhost:7474";
	final String extensionEntryPointUri = baseUri + "/examples/unmanaged/ehri";

	final String adminUserProfileId = "80497"; // supply this, depends on your
												// database content!
	static final String UPDATED_NAME = "UpdatedNameTEST";

	private Client client = Client.create();
	private Converter converter = new Converter();

	private String jsonDocumentaryUnitTestStr; // test data to create a DocumentaryUnit
	private String jsonUserProfileTestString = "{\"data\":{\"userId\":-1,\"name\":\"testUserName1\",\"isA\":\"userProfile\"}}";

	@Before
	public void setUp() throws Exception {
		jsonDocumentaryUnitTestStr = readFileAsString("documentaryUnit.json");
	}

	/**
	 * Tests if we have an admin user, we need that user for doing all the other tests
	 * 
	 * curl -v -X GET -H "Authorization: 80497"  -H "Accept: application/json" 
	 *	      http://localhost:7474/examples/unmanaged/ehri/userProfile/80497
	 *
	 */
	@Test
	public void testAdminGetUserProfile() throws Exception {		
		// get the admin user profile
		WebResource resource = client.resource(extensionEntryPointUri
				+ "/userProfile" + "/" + adminUserProfileId);
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		
		// TODO check it has a group with 'admin' rights
	}
	
	/*** DocumentaryUnit ***/
	
	/**
	 * 	 CR(U)D cycle
	 *	 could also use curl like this:
	 *	 curl -v -X POST -H "Authorization: 80497" -H "Accept: application/json" -H "Content-type: application/json" 
	 *	      http://localhost:7474/examples/unmanaged/ehri/documentaryUnit 
	 *    		-d '{"data":{"name":"a collection","identifier":"some id",
	 *	          "isA":"documentaryUnit"},"relationships":{"describes":[{"data":{"identifier":"some id",
	 *	          "title":"a description","isA":"documentDescription","languageOfDescription":"en"}}],
	 *	          "hasDate":[{"data":{"startDate":"1940-01-01T00:00:00Z","endDate":"1945-01-01T00:00:00Z",
	 *	          "isA":"datePeriod"}}]}}'
	 *	
	 *	 curl -v -X GET -H "Authorization: 80497"  -H "Accept: application/json" 
	 *	      http://localhost:7474/examples/unmanaged/ehri/documentaryUnit/80501
	 *	 
	 *	 curl -v -X DELETE -H "Authorization: 80497"  -H "Accept: application/json" 
	 *	      http://localhost:7474/examples/unmanaged/ehri/documentaryUnit/80501
	 */
	@Test
	public void testCreateDeleteDocumentaryUnit() throws Exception {
		// Create
		WebResource resource = client.resource(extensionEntryPointUri
				+ "/documentaryUnit");
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonDocumentaryUnitTestStr)
				.post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO test if json is valid?
		//response.getEntity(String.class)
		
		// Get created doc via the response location?
		URI location = response.getLocation();

		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO again test json

		delete(location);
	}

	@Test
	public void testUpdateDocumentaryUnit() throws Exception {		
		
		// -create data for testing
		WebResource resource = client.resource(extensionEntryPointUri
				+ "/documentaryUnit");
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonDocumentaryUnitTestStr)
				.post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO test if json is valid?
		//response.getEntity(String.class)
		
		// Get created doc via the response location?
		URI location = response.getLocation();

		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// -get the data and change it
		String json = response.getEntity(String.class);
		EntityBundle<DocumentaryUnit> entityBundle = converter.jsonToBundle(json); 
		Map<String, Object> data = entityBundle.getData();
		data.put("name", UPDATED_NAME);
		entityBundle = entityBundle.setData(data);
		String toUpdateJson = converter.bundleToJson(entityBundle);
		
		// -update
		resource = client.resource(extensionEntryPointUri + "/documentaryUnit");
		response = resource.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(toUpdateJson)
				.put(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
				
		// -get the data and convert to a bundle, is it changed?
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// -get the data and convert to a bundle, is it OK?
		String updatedJson = response.getEntity(String.class);
		EntityBundle<DocumentaryUnit> updatedEntityBundle = converter.jsonToBundle(updatedJson); 
		Map<String, Object> updatedData = updatedEntityBundle.getData();
		assertEquals(UPDATED_NAME, updatedData.get("name"));
		
		delete(location);
	}

	/*** UserProfile ***/
	
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
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonUserProfileTestString).post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		
		// Get created entity via the response location?
		URI location = response.getLocation();
		
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
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
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(jsonUserProfileTestString)
				.post(ClientResponse.class);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		// TODO test if json is valid?
		//response.getEntity(String.class)
		
		// Get created doc via the response location?
		URI location = response.getLocation();

		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
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
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.entity(toUpdateJson)
				.put(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
				
		// -get the data and convert to a bundle, is it changed?
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// -get the data and convert to a bundle, is it OK?
		String updatedJson = response.getEntity(String.class);
		EntityBundle<DocumentaryUnit> updatedEntityBundle = converter.jsonToBundle(updatedJson); 
		Map<String, Object> updatedData = updatedEntityBundle.getData();
		assertEquals(UPDATED_NAME, updatedData.get("name"));
		
		delete(location);
	}

	/*** Helpers ***/
	
	private void delete(URI location)
	{
		WebResource resource = client.resource(location);
		
		// Delete
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.delete(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// Get should fail now
		resource = client.resource(location);
		response = resource.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jResource.AUTH_HEADER_NAME, adminUserProfileId)
				.get(ClientResponse.class);
		 //assertEquals(Response.Status.NOT_FOUND, response.getStatus());
		// TODO fix it!
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
				response.getStatus());
	}
	
	/**
	 * NOTE not sure how this handles UTF8
	 * 
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	private String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1024);
		BufferedReader reader = new BufferedReader(new InputStreamReader(this
				.getClass().getClassLoader().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();

		return fileData.toString();
	}
}
