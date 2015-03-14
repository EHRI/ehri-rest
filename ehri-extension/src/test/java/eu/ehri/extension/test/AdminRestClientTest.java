package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AdminResource;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static eu.ehri.extension.AdminResource.ENDPOINT;

/**
 * Test admin REST functions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AdminRestClientTest extends BaseRestClientTest {

    @Test
    public void testCreateDefaultUser() throws Exception {
        // Create
        WebResource resource = client.resource(ehriUri(ENDPOINT, "createDefaultUserProfile"));
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertStatus(CREATED, response);
        String json = response.getEntity(String.class);
        Bundle bundle = Bundle.fromString(json);
        String ident = (String) bundle.getData().get(Ontology.IDENTIFIER_KEY);
        assertTrue(ident != null);
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

        // Create another user and ensure their idents are different and
        // incremental
        WebResource resource2 = client.resource(ehriUri(ENDPOINT, "createDefaultUserProfile"));
        response = resource2.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);

        assertStatus(CREATED, response);
        String json2 = response.getEntity(String.class);
        Bundle bundle2 = Bundle.fromString(json2);
        String ident2 = (String) bundle2.getData().get(
                Ontology.IDENTIFIER_KEY);
        assertEquals(parseUserId(ident) + 1L, parseUserId(ident2));
        assertTrue(ident.startsWith(AdminResource.DEFAULT_USER_ID_PREFIX));

    }

    // Helpers
    private long parseUserId(String ident) {
        return Long.parseLong(ident.replace(
                AdminResource.DEFAULT_USER_ID_PREFIX, ""));
    }
}
