package eu.ehri.extension.test;

import static eu.ehri.extension.UserProfileResource.WATCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.EventResource;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
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
    public void testListActionsWithFilter() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.

        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.COUNTRY + "/" + COUNTRY_CODE + "/" +  Entities.REPOSITORY);
        resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .post(ClientResponse.class);

        // Add a good user filter...
        MultivaluedMap<String,String> goodFilters = new MultivaluedMapImpl();
        goodFilters.add(EventResource.USER_PARAM, getAdminUserProfileId());

        // Add a useless filter that should remove all results
        MultivaluedMap<String,String> badFilters = new MultivaluedMapImpl();
        badFilters.add(EventResource.USER_PARAM, "nobody");

        List<Map<String, Object>> goodFiltered = getEntityList(
                Entities.SYSTEM_EVENT, getAdminUserProfileId(), goodFilters);

        List<Map<String, Object>> badFiltered = getEntityList(
                Entities.SYSTEM_EVENT, getAdminUserProfileId(), badFilters);

        assertTrue(goodFiltered.size() > 0);
        assertEquals(0, badFiltered.size());
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

    @Test
    public void testPersonalisedEventList() throws Exception {

        // Create an event by updating an item...
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.REPOSITORY + "/" + "r1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAgentTestString)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // At present, the personalised event stream for the validUser user should
        // be empty because she's not watching any items

        String user = getRegularUserProfileId();
        String personalisedEventUrl = "/" + Entities.SYSTEM_EVENT + "/forUser/" + user;
        List<Map<String, Object>> events = getItemList(personalisedEventUrl, user);
        assertTrue(events.isEmpty());

        // Now start watching item r1
        URI watchUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user)
                .segment(WATCH)
                .segment("r1")
                .build();

        client.resource(watchUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user)
                .post(ClientResponse.class);

        // Now our event list should contain one item...
        events = getItemList(personalisedEventUrl, user);
        assertEquals(1, events.size());
    }
}
