package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistance.Bundle;

public class AgentRestClientTest extends BaseRestClientTest {

    static final String UPDATED_ID = "r1";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String agentTestData;
    private String badAgentTestData = "{\"data\":{\"identifier\": \"jmp\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AgentRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        agentTestData = readFileAsString("agent.json");
    }

    @Test
    public void testCreateAgent() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(agentTestData)
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
    public void testUpdateAgentByIdentifier() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(agentTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Obtain some update data.
        String updateData = Bundle.fromString(agentTestData)
                .withDataValue("name", UPDATED_NAME).toString();

        resource = client.resource(response.getLocation());
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(updateData)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateAgentWithDeserializationError() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(badAgentTestData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        // Check the JSON gives use the correct error
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("error");
        assertFalse(errValue.isMissingNode());
        assertEquals(DeserializationError.class.getSimpleName(),
                errValue.asText());
    }

    @Test
    public void testDeleteAgent() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/agent/r1");
        ClientResponse response = resource.header(
                AbstractRestResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .delete(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Check it's really gone...
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                response.getStatus());
    }
}
