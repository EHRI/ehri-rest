package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;

public class AccessRestClientTest extends BaseRestClientTest {

    static final String LIMITED_USER_NAME = "reto";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AccessRestClientTest.class.getName());
    }

    @Test
    public void testUserCannotRead() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testGrantAccess() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Laboriously fetch the ids of the user and the item, respectively
        String userJson = client
                .resource(
                        getExtensionEntryPointUri() + "/userProfile/"
                                + LIMITED_USER_NAME)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(String.class);
        Long userId = new ObjectMapper().readValue(userJson, JsonNode.class)
                .path("id").asLong();

        String itemJson = client
                .resource(getExtensionEntryPointUri() + "/documentaryUnit/c1")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(String.class);
        Long itemId = new ObjectMapper().readValue(itemJson, JsonNode.class)
                .path("id").asLong();

        // Set the form data
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("accessor", userId.toString());
        resource = client.resource(getExtensionEntryPointUri() + "/access/"
                + itemId);
        response = resource
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class, formData);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Try the original request again and ensure it worked...
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

}
