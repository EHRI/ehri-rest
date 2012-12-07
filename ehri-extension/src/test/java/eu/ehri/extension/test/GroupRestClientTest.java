package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

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

    private String jsonGroupTestString = "{\"type\": \"group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(GroupRestClientTest.class.getName());
    }

    @Test
    public void testCreateDeleteGroup() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/group");
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
}
