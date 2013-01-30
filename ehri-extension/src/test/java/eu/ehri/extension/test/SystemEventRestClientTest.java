package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;

public class SystemEventRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonAgentTestString = "{\"type\": \"agent\", \"data\":{\"identifier\": \"jmp\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(SystemEventRestClientTest.class.getName());
    }

    @Test
    public void testListActions() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.

        List<Map<String, Object>> actionsBefore = getEntityList(
                Entities.SYSTEM_EVENT, getAdminUserProfileId());

        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        List<Map<String, Object>> actionsAfter = getEntityList(
                Entities.SYSTEM_EVENT, getAdminUserProfileId());

        // Having created a new Agent, we should have at least one Event.
        assertEquals(actionsBefore.size() + 1, actionsAfter.size());
    }
    
    @Test
    public void testGetActionsForItem() throws Exception {

        // Create an item
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .post(ClientResponse.class);
        
        // Get the id
        String url = response.getLocation().toString();
        String id = url.substring(url.lastIndexOf("/") + 1);
        
        resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.SYSTEM_EVENT + "/for/" + id);
        
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
    }
}
