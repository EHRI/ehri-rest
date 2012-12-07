package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AdminResource;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Bundle;

/**
 * Test admin REST functions.
 * 
 * @author michaelb
 * 
 */
public class AdminRestClientTest extends BaseRestClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AdminRestClientTest.class.getName());
    }

    @Test
    public void testCreateDefaultUser() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/admin/createDefaultUserProfile");
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        String json = response.getEntity(String.class);
        Bundle bundle = converter.jsonToBundle(json);
        String ident = (String) bundle.getData().get(
                AccessibleEntity.IDENTIFIER_KEY);
        assertTrue(ident != null);
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

        // Create another user and ensure their idents are different and
        // incremental
        WebResource resource2 = client.resource(getExtensionEntryPointUri()
                + "/admin/createDefaultUserProfile");
        ClientResponse response2 = resource2.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response2.getStatus());
        String json2 = response2.getEntity(String.class);
        Bundle bundle2 = converter.jsonToBundle(json2);
        String ident2 = (String) bundle2.getData().get(
                AccessibleEntity.IDENTIFIER_KEY);
        assertEquals(parseUserId(ident) + 1L, parseUserId(ident2));
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

    }

    // Helpers
    private long parseUserId(String ident) {
        return Long.parseLong(ident.replace(
                AdminResource.DEFAULT_USER_ID_PREFIX, ""));
    }
}
