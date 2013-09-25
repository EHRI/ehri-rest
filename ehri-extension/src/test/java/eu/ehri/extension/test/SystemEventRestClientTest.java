package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistance.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;


public class SystemEventRestClientTest extends BaseRestClientTest {

    static final String COUNTRY_CODE = "nl";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonAgentTestString = "{\"type\": \"repository\", \"data\":{\"identifier\": \"jmp\"}}";

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
                + "/" + Entities.COUNTRY + "/" + COUNTRY_CODE + "/" +  Entities.REPOSITORY);
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

        // Having created a new Repository, we should have at least one Event.
        assertEquals(actionsBefore.size() + 1, actionsAfter.size());
    }
    
    @Test
    public void testGetActionsForItem() throws Exception {

        // Create an item
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.COUNTRY + "/" + COUNTRY_CODE + "/" + Entities.REPOSITORY);
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
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetVersionsForItem() throws Exception {

        // Create an item
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.REPOSITORY + "/" + "r1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.SYSTEM_EVENT + "/versions/r1");

        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        String json = response.getEntity(String.class);
        // Check the response contains a new version
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(json, JsonNode.class);
        assertFalse(rootNode.path("values").path(0).path(Bundle.DATA_KEY)
                .path(Ontology.VERSION_ENTITY_DATA).isMissingNode());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
