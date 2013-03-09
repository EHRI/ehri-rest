package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.persistance.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
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

    private static final String ITEM = "cd1";

    @Test
    public void getGeneticEntity() throws IOException {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/entities?id=" + ITEM);
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
        assertEquals(ITEM, idValue.getTextValue());
        // ensure only one item was returned...
        assertTrue(rootNode.path(1).isMissingNode());
    }
}
