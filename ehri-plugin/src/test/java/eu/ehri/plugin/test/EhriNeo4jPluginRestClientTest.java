package eu.ehri.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.plugin.test.utils.ServerRunner;

/**
 * The unit testing is done in the ehri-frames project because the
 * EhriNeo4jPlugin is just a thin wrapper around the EhriNeo4j class in the
 * ehri-frames project.
 * 
 */
public class EhriNeo4jPluginRestClientTest {
    private static final Logger logger = LoggerFactory
            .getLogger(EhriNeo4jPluginRestClientTest.class);

    final String baseUri = "http://localhost:7575";
    final String pluginEntryPointUri = baseUri + "/db/data/ext/EhriNeo4jPlugin";

    protected static ServerRunner runner;

    @BeforeClass
    public static void setupServer() {
        runner = new ServerRunner(
                EhriNeo4jPluginRestClientTest.class.getName(), 7575);
        runner.start();
    }

    /**
     * Shut down database when test suite has run.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void shutdownDatabase() throws Exception {
        runner.stop();
    }

    @Test
    public void createInitialAdmin() throws Exception {
        final String vertexUri = pluginEntryPointUri
                + "/graphdb/createIndexedVertex";
        final String vertexIndexUri = pluginEntryPointUri
                + "/graphdb/getOrCreateVertexIndex";
        final String edgeUri = pluginEntryPointUri
                + "/graphdb/createIndexedEdge";
        final String edgeIndexUri = pluginEntryPointUri
                + "/graphdb/getOrCreateEdgeIndex";

        Client client = Client.create();

        // create an admin group vertex
        WebResource vertexIndexResource = client.resource(vertexIndexUri);
        ClientResponse response = vertexIndexResource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"group\", \"parameters\": {}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        WebResource vertexResource = client.resource(vertexUri);
        response = vertexResource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"group\", \"data\": {\"name\": \"admin\", \"isA\": \"group\"}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Long groupId = getIdFromResponseString(response.getEntity(String.class));

        // create an admin userProfile vertex
        vertexIndexResource = client.resource(vertexIndexUri);
        response = vertexIndexResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"userProfile\", \"parameters\": {}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response = vertexResource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"userProfile\", \"data\": {\"userId\": 0, \"name\": \"admin\", \"isA\": \"userProfile\"}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Long userProfileId = getIdFromResponseString(response
                .getEntity(String.class));

        // create a belongsTo edge from user to group
        WebResource edgeIndexResource = client.resource(edgeIndexUri);
        response = edgeIndexResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"belongsTo\", \"parameters\": {}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        WebResource edgeResource = client.resource(edgeUri);
        response = edgeResource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"belongsTo\"" + ", \"outV\": "
                        + userProfileId + ", \"inV\": " + groupId
                        + ", \"typeLabel\": \"belongsTo\" , \"data\": {}}")
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    /**
     * Get the id of a value from its JSON string representation.
     * 
     * @param responseStr
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    private Long getIdFromResponseString(String responseStr)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(responseStr, Map.class);
        String self = (String) data.get("self");
        return Long.valueOf(self.substring(self.lastIndexOf('/') + 1));
    }

    /**
     * Test creation of an indexed Vertex (via the plugin)
     */
    @Test
    public void testCreateIndexedVertex() {
        System.out.println("Using neo4jPlugin RESTfull interface from Java");

        final String makeIndexUri = pluginEntryPointUri
                + "/graphdb/getOrCreateVertexIndex";

        final String entryPointUri = pluginEntryPointUri
                + "/graphdb/createIndexedVertex";

        Client client = Client.create();

        // Create an index - we don't care about the response.
        client.resource(makeIndexUri).accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\": \"test\", \"parameters\": {}}")
                .post(ClientResponse.class).close();

        WebResource resource = client.resource(entryPointUri);

        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"index\":\"test\", \"data\": {\"name\": \"Test1\"}}")
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // lets test a request with missing input parameters
        response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).entity("{}")
                .post(ClientResponse.class);
        // Unsuccessful bad request, or at least not OK ?
        assertFalse(response.getStatus() == Response.Status.OK.getStatusCode());

        // TODO logging
        System.out.println(String.format("POST to [%s], status code [%d]",
                entryPointUri, response.getStatus()));
        System.out.println(response.getEntity(String.class));

        response.close();
    }

    /*** TODO more tests ***/

    /**
     * Using neo4j RESTfull interface from Java, just to see that existing REST
     * works without the plugin
     */
    @Test
    public void testCreateByNeo4j() {
        final String nodeEntryPointUri = baseUri + "/db/data/node";
        // System.out.println ("Using neo4j RESTfull interface from Java");

        // Instantiate Jersey's Client class
        Client client = Client.create(); // why not with new?
        WebResource resource = client.resource(nodeEntryPointUri);

        // this is a create on the Neo4j REST API
        // POST {} to the node entry point URI
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).entity("{}")
                .post(ClientResponse.class);

        // final URI location = response.getLocation();
        // System.out.println( String.format(
        // "POST to [%s], status code [%d], location header [%s]",
        // nodeEntryPointUri, response.getStatus(), location.toString() ) );

        response.close();
    }
}
