package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Bundle;

public class UserProfileRestClientTest extends BaseRestClientTest {

    static final String FETCH_NAME = "mike";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonUserProfileTestString = "{\"type\":\"userProfile\", \"data\":{\"identifier\": \"test-user\", \"name\":\"testUserName1\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(UserProfileRestClientTest.class.getName());
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteUserProfile() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/userProfile");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get created entity via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
    }

    @Test
    public void testGetByKeyValue() throws Exception {
        // -create data for testing
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("key", AccessibleEntity.IDENTIFIER_KEY);
        queryParams.add("value", FETCH_NAME);

        WebResource resource = client.resource(
                getExtensionEntryPointUri() + "/userProfile").queryParams(
                queryParams);

        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testUpdateUserProfile() throws Exception {

        // -create data for testing
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/userProfile");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonUserProfileTestString).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and change it
        String json = response.getEntity(String.class);
        Bundle entityBundle = Bundle.fromString(json).withDataValue("name",
                UPDATED_NAME);

        // -update
        resource = client
                .resource(getExtensionEntryPointUri() + "/userProfile");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(entityBundle.toString()).put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }
}
