package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static junit.framework.Assert.*;
import org.junit.Ignore;

public class GenericRestClientTest extends BaseRestClientTest {

    private static final String ITEM1 = "c1";
    private static final String ITEM2 = "c4";
    private static final String BAD_ITEM = "cd1";

    @Test
    public void getMultipleGenericEntities() throws IOException {
        // Create
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment("entities")
                .queryParam("id", ITEM1)
                .queryParam("id", ITEM2).build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getMultipleGenericEntitiesByPost() throws IOException {
        // Create
        String payload = String.format("[\"%s\", \"%s\"]", ITEM1, ITEM2);
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment("entities").build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(payload)
                .post(ClientResponse.class);
        assertStatus(OK, response);
        testResponse(response, ITEM1);
    }

    @Test
    public void getSingleGenericEntity() throws IOException {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("entities", ITEM1)).get(ClientResponse.class);
        assertStatus(OK, response);
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(ITEM1, idValue.getTextValue());
    }

    @Test
    @Ignore
    public void getCannotFetchNonContentTypes() throws IOException {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("entities", BAD_ITEM)).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void listEntitiesByGidThrows404() throws IOException {
        // Create
        URI uri = UriBuilder.fromUri(getExtensionEntryPointUri())
                .segment("entities")
                .segment("listByGraphId")
                .queryParam("gid", -1L).build();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    private void testResponse(ClientResponse response, String expectedId) throws IOException {
        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(expectedId, idValue.getTextValue());
        // ensure only one item was returned...
        assertFalse(rootNode.path(1).isMissingNode());
        assertTrue(rootNode.path(2).isMissingNode());
    }
}
