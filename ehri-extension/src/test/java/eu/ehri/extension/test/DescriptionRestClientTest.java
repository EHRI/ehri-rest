package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

public class DescriptionRestClientTest extends BaseRestClientTest {

    private String descriptionTestStr;
    static final String TEST_DESCRIPTION_IDENTIFIER = "another-description";
    // FIXME: This ID is temporaty and will break when we decide on a proper

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(DescriptionRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        descriptionTestStr = readFileAsString("documentDescription.json");
    }

    @Test
    public void testCreateDescription() throws Exception {
        // Create additional description for c2
        // C2 initially has one description, so it should have two afterwards
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/description/c2");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(descriptionTestStr).post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode
                .path(Bundle.DATA_KEY)
                .path(Ontology.IDENTIFIER_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(TEST_DESCRIPTION_IDENTIFIER, idValue.getTextValue());
    }

    @Test
    public void testUpdateDescription() throws Exception {
        // Update description for c2
        // C2 initially has one description, and should still have one afterwards
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/description/c2/cd2");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(descriptionTestStr).put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idValue = rootNode
                .path(Bundle.DATA_KEY)
                .path(Ontology.IDENTIFIER_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals(TEST_DESCRIPTION_IDENTIFIER, idValue.getTextValue());
        // Assert there are no extra descriptions
        assertTrue(rootNode.path(Bundle.REL_KEY).path(
                Ontology.DESCRIPTION_FOR_ENTITY).path(1).isMissingNode());
    }

    @Test
    public void testDeleteDescription() throws Exception {
        // Delete description for c2
        // C2 initially has one description, so there should be none afterwards
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/description/c2/cd2");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
