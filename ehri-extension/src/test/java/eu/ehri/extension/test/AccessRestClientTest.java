package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static com.sun.jersey.api.client.ClientResponse.Status.UNAUTHORIZED;

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
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(UNAUTHORIZED, response);
    }

    @Test
    public void testGrantAccess() throws Exception {
        // Attempt to fetch an element.
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(UNAUTHORIZED, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        response = client.resource(ehriUri("access", "c1"))
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testRevokeAccess() throws Exception {

        // first, grant access
        testGrantAccess();

        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(OK, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME);

        WebResource resource = client.resource(ehriUri("access", "c1"));
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(UNAUTHORIZED, response);
    }

}
