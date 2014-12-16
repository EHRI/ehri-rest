package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;

/**
 * Test admin REST functions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ToolsRestClientTest extends BaseRestClientTest {
    @Test
    public void testPropertyRename() throws Exception {
        WebResource resource = client.resource(ehriUri("tools", "_findReplacePropertyValue"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("name", "streetAddress")
                .queryParam("from", "Strand")
                .queryParam("to", "Drury Lane");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("1", response.getEntity(String.class));
    }

    @Test
    public void testPropertyRenameRE() throws Exception {
        WebResource resource = client.resource(ehriUri("tools", "_findReplacePropertyValueRE"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("name", "webpage")
                .queryParam("pattern", "^http:")
                .queryParam("replace", "https:");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("3", response.getEntity(String.class));
    }

    @Test
    public void testPropertyKeyRename() throws Exception {
        WebResource resource = client.resource(ehriUri("tools", "_findReplacePropertyName"))
                .queryParam("type", Entities.ADDRESS)
                .queryParam("from", "streetAddress")
                .queryParam("to", "somethingElse");
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals("2", response.getEntity(String.class));
    }
}
