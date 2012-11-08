package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.EhriNeo4jFramedResource;

public class BasicFramedVertexRestClientTest extends BaseRestClientTest {

    private String jsonTestString = "{\"data\":{\"isA\": \"basic\",\"identifier\": \"xxx\",\"unknown\": \"something\"}}";
    // Needs  a isA:basic, and also an 'identifier' property
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(BasicFramedVertexRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCreate() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/basic");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                		getAdminUserProfileId()).entity(jsonTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                		getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
