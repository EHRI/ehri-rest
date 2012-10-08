package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.project.models.EntityTypes;

public class ActionRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonAgentTestString = "{\"data\":{\"isA\": \"agent\", \"identifier\": \"jmp\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(ActionRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testListActions() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.

        List<Map<String, Object>> actionsBefore = getEntityList(
                EntityTypes.ACTION, getAdminUserProfileId());

        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        List<Map<String, Object>> actionsAfter = getEntityList(
                EntityTypes.ACTION, getAdminUserProfileId());

        // Having created a new Agent, we should have at least one Action.
        assertEquals(actionsBefore.size() + 1, actionsAfter.size());
    }
}
