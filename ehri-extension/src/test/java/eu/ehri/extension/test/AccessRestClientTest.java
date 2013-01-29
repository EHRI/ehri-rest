package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;

public class AccessRestClientTest extends BaseRestClientTest {

    static final String PRIVILEGED_USER_NAME = "mike";
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
        // Attempt to fetch an element.
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Set the form data
        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        resource = client.resource(getExtensionEntryPointUri() + "/access/c1");
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
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

    @Test
    public void testRevokeAccess() throws Exception {

        // first, grant access
        testGrantAccess();

        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Set the form data
        MultivaluedMap<String,String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME);

        resource = client.resource(getExtensionEntryPointUri() + "/access/c1");
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Try the original request again and ensure it worked...
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/c1");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
    }

}
