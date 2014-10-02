package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AdminResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.INTERNAL_SERVER_ERROR;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test admin REST functions.
 *
 * @author michaelb
 */
public class AdminRestClientTest extends BaseRestClientTest {

    @Test
    public void testHouseKeeping() throws Exception {
        WebResource resource = client.resource(ehriUri("admin", "_rebuildChildCache"));
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testPropertyRename() throws Exception {
        WebResource resource = client.resource(ehriUri("admin", "_findReplacePropertyValue"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("name", "streetAddress")
                .queryParam("from", "Strand")
                .queryParam("to", "Drury Lane");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("1", response.getEntity(String.class));

        // check it fails for invalid properties...
        for (String badProp: new String[]{EntityType.ID_KEY, EntityType.TYPE_KEY}) {
            resource = client.resource(ehriUri("admin", "_findReplacePropertyValue"))
                    .queryParam("type", Entities.ADDRESS)
                    .queryParam("name", badProp)
                    .queryParam("from", "foo")
                    .queryParam("to", "bar");
            response = resource
                    .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
            assertStatus(INTERNAL_SERVER_ERROR, response);
        }
    }

    @Test
    public void testPropertyKeyRename() throws Exception {
        WebResource resource = client.resource(ehriUri("admin", "_findReplacePropertyName"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("from", "streetAddress")
                .queryParam("to", "somethingElse");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("2", response.getEntity(String.class));
    }

    @Test
    public void testCreateDefaultUser() throws Exception {
        // Create
        WebResource resource = client.resource(ehriUri("admin", "createDefaultUserProfile"));
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
        WebResource resource2 = client.resource(ehriUri("admin", "createDefaultUserProfile"));
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
