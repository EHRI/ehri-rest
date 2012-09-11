package eu.ehri.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * The unit testing is done in the ehri-data project because  
 * the EhriNeo4jPlugin is just a thin wrapper around the EhriNeo4j class in the ehri-data project. 
 * 
 * The test by EhriNeo4jPluginRestClientTest will need to have a running neo4j server with the plugin installed. 
 * It will call the REST interface using the Jersey neo4j client, but the same could have been done using curl. 
 * However, we then would have shell scripts with curl calls, and having the test in Java is more portable. 
 * Also it shows how you can use the REST api from your Java program. 
 * 
 */
public class EhriNeo4jPluginRestClientTest {
	private static final Logger logger = LoggerFactory.getLogger(EhriNeo4jPluginRestClientTest.class);

	final String baseUri = "http://localhost:7474";
	final String pluginEntryPointUri = baseUri + "/db/data/ext/EhriNeo4jPlugin";

	@Test
	public void createInitialAdmin() throws Exception
	{
		final String vertexUri = pluginEntryPointUri+"/graphdb/createIndexedVertex";
		final String vertexIndexUri = pluginEntryPointUri+"/graphdb/getOrCreateVertexIndex";
		final String edgeUri = pluginEntryPointUri+"/graphdb/createIndexedEdge";
		final String edgeIndexUri = pluginEntryPointUri+"/graphdb/getOrCreateEdgeIndex";
		
		Client client = Client.create();

		// create an admin group vertex
		WebResource vetrexIndexResource = client.resource(vertexIndexUri);
		ClientResponse response = vetrexIndexResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"group\", \"parameters\": {}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());				
		WebResource vertexResource = client.resource(vertexUri);
		response = vertexResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"group\", \"data\": {\"name\": \"admin\", \"isA\": \"group\"}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		Long groupId = getIdFromResponseString(response.getEntity(String.class));
		
		// create an admin userProfile vertex
		vetrexIndexResource = client.resource(vertexIndexUri);
		response = vetrexIndexResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"userProfile\", \"parameters\": {}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		response = vertexResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"userProfile\", \"data\": {\"userId\": 0, \"name\": \"admin\", \"isA\": \"userProfile\"}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		Long userProfileId = getIdFromResponseString(response.getEntity(String.class));

		// create a belongsTo edge from user to group
		WebResource edgeIndexResource = client.resource(edgeIndexUri);
		response = edgeIndexResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"belongsTo\", \"parameters\": {}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		WebResource edgeResource = client.resource(edgeUri);
		response = edgeResource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"belongsTo\"" +
		        		", \"outV\": "  + userProfileId  + 
		        		", \"inV\": "  + groupId  + 
		        		", \"typeLabel\": \"belongsTo\" , \"data\": {}}" )
		        .post( ClientResponse.class );
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}

	/**
	 * 
	 * @param responseStr
	 * @return
	 */
	private Long getIdFromResponseString(String responseStr)
	{
		// Using regexp to get the id from the string
		// Note; could do json parsing, if needed
		// ObjectMapper mapper = new ObjectMapper();
		// JsonNode jsonResp = mapper.readTree(response.getEntity(String.class));

		Long id = null;
		
		logger.debug("getting id from string: " + "\"" + responseStr + "\"");
		
		// for example get 1234 from ""v[1234]""
		// Note that then string is quoted
		Pattern p = Pattern.compile("\\\"v\\[(\\d+)\\]\\\"");
		Matcher m = p.matcher(responseStr);
		if (m.matches())
		{
			String idStr = m.group(1);
			id = Long.parseLong(idStr);
		}
		
		return id;
	}
	
	/** 
	 * Test creation of an indexed Vertex (via the plugin)
	 * 
	 * Example with curl:
	 * curl -X POST -H "Content-type: application/json" 
	 *   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedVertex 
	 *   -d '{"index":"test", "data": {"name": "Test1"}}'
	 */
	@Test
	public void testCreateIndexedVertex()
	{
		System.out.println ("Using neo4jPlugin RESTfull interface from Java");
		final String entryPointUri = pluginEntryPointUri+"/graphdb/createIndexedVertex";
		
		Client client = Client.create();
		WebResource resource = client.resource(entryPointUri);
		
		ClientResponse response = resource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{\"index\":\"test\", \"data\": {\"name\": \"Test1\"}}" )
		        .post( ClientResponse.class );
		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		
		// lets test a request with missing input parameters
		response = resource.accept( MediaType.APPLICATION_JSON  )
					.type( MediaType.APPLICATION_JSON  )
			        .entity( "{}" )
			        .post( ClientResponse.class );
		// Unsuccessful bad request, or at least not OK ?
		assertFalse(response.getStatus() == Response.Status.OK.getStatusCode());
		
		// TODO logging
		System.out.println( String.format(
		        "POST to [%s], status code [%d]",
		        entryPointUri, response.getStatus()) );
		System.out.println(response.getEntity(String.class));
	
		response.close();
	}

	/*** TODO more tests ***/

	/**
	 * Using neo4j RESTfull interface from Java, 
	 * just to see that existing REST works without the plugin
	 */
	@Test
	public void testCreateByNeo4j()
	{
		final String nodeEntryPointUri = baseUri + "/db/data/node";
		//System.out.println ("Using neo4j RESTfull interface from Java");
		
		// Instantiate Jersey's Client class
		Client client = Client.create(); // why not with new?	
		WebResource resource = client.resource(nodeEntryPointUri);
		
		// this is a create on the Neo4j REST API
		// POST {} to the node entry point URI
		ClientResponse response = resource.accept( MediaType.APPLICATION_JSON  )
				.type( MediaType.APPLICATION_JSON  )
		        .entity( "{}" )
		        .post( ClientResponse.class );
		
		//final URI location = response.getLocation();
		//System.out.println( String.format(
		//        "POST to [%s], status code [%d], location header [%s]",
		//        nodeEntryPointUri, response.getStatus(), location.toString() ) );
	
		response.close();
	}

}
