package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistance.Bundle;

public class AgentRestClientTest extends BaseRestClientTest {

    static final String ID = "r1";
    static final String LIMITED_USER_NAME = "reto";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String agentTestData;
    private String docTestData;
    private String badAgentTestData = "{\"data\":{\"identifier\": \"jmp\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AgentRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        agentTestData = readFileAsString("agent.json");
        docTestData = readFileAsString("documentaryUnit.json");
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

    @Test
    public void testGrantPermsForAgentScope() throws Exception {
        // Grant permissions for a user to create items within this scope.

        // The user shouldn't be able to create docs with r2
        WebResource resource = client.resource(getCreationUriFor("r2"));
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(docTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Or r3...
        resource = client.resource(getCreationUriFor("r3"));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(docTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Now grant the user permissions to create just within
        // the scope of r2
        String permData = "{\"documentaryUnit\": [\"create\"]}";

        URI grantUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.PERMISSION)
                .segment(LIMITED_USER_NAME)
                .segment("scope")
                .segment("r2").build();

        resource = client.resource(grantUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(permData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Now creation should succeed...
        resource = client.resource(getCreationUriFor("r2"));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(docTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // But r3 should still fail...
        // Or r3...
        resource = client.resource(getCreationUriFor("r3"));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(docTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // And the user himself should not be able to grant
        // others the ability to create within that scope.
        String otherUserName = "linda";
        String grantPermData = "{\"documentaryUnit\": [\"grant\"]}";
        URI otherGrantUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.PERMISSION)
                .segment(otherUserName)
                .segment("scope")
                .segment("r2").build();

        resource = client.resource(otherGrantUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(grantPermData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

    }

    private URI getCreationUriFor(String id) {
        URI creationUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.AGENT).segment(id)
                .segment(Entities.DOCUMENTARY_UNIT).build();
        return creationUri;
    }

}
