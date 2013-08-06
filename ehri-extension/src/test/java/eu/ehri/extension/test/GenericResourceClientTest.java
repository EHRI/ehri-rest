package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.persistance.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: mikebryant
 * Date: 09/03/2013
 * Time: 14:36
 * To change this template use File | Settings | File Templates.
 */
public class GenericResourceClientTest  extends BaseRestClientTest {

    private static final String ITEM1 = "c1";
    private static final String ITEM2 = "c4";
    private static final String BAD_ITEM = "cd1";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(GenericResourceClientTest.class.getName());
    }

    @Test
    public void getMultipleGenericEntities() throws IOException {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/entities?id=" + ITEM1 + "&id=" + ITEM2);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.getTextValue());
        // ensure only one item was returned...
        assertFalse(rootNode.path(1).isMissingNode());
        assertTrue(rootNode.path(2).isMissingNode());
    }

    @Test
    public void getSingleGenericEntity() throws IOException {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/entities/" + ITEM1);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.getTextValue());
    }

    @Test
    public void getCannotFetchNonContentTypes() throws IOException {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/entities/" + BAD_ITEM);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                response.getStatus());
    }
}
