package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static junit.framework.Assert.*;

public class GenericResourceClientTest extends BaseRestClientTest {

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
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment("entities")
                .queryParam("id", ITEM1)
                .queryParam("id", ITEM2).build();
        System.out.println("URI: " + uri);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
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
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("entities", ITEM1)).get(ClientResponse.class);
        assertStatus(OK, response);
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
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("entities", BAD_ITEM)).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }
}
