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

public class GroupRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonGroupTestString = "{\"data\":{\"isA\": \"group\", \"identifier\": \"niod\", \"name\": \"NIOD\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(GroupRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCreateDeleteGroup() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/group");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                		getAdminUserProfileId()).entity(jsonGroupTestString)
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
