package eu.ehri.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
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
	final String baseUri = "http://localhost:7474";
	final String pluginEntryPointUri = baseUri + "/db/data/ext/EhriNeo4jPlugin";

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
		
		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
		
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
