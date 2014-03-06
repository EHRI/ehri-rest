package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.*;

public class DescriptionRestClientTest extends BaseRestClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private String descriptionTestStr;
    private String accessPointTestStr;
    static final String TEST_DESCRIPTION_IDENTIFIER = "another-description";
    // FIXME: This ID is temporaty and will break when we decide on a proper

    @Before
    public void setUp() throws Exception {
        descriptionTestStr = readFileAsString("documentDescription.json");
        accessPointTestStr = readFileAsString("undeterminedRelationship.json");
    }

    @Test
    public void testCreateDescription() throws Exception {
        // Create additional description for c2
        // C2 initially has one description, so it should have two afterwards
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("description", "c2"))
                .entity(descriptionTestStr).post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);

        // Check ID is the correct concatenation of all the scope IDs...
        JsonNode idValue = rootNode
                .path(Bundle.ID_KEY);
        assertFalse(idValue.isMissingNode());
        assertEquals("nl-r1-c1-c2-en-another-description", idValue.getTextValue());

        // Check the identifier is present and correct...
        JsonNode identValue = rootNode
                .path(Bundle.DATA_KEY)
                .path(Ontology.IDENTIFIER_KEY);
        assertFalse(identValue.isMissingNode());
        assertEquals(TEST_DESCRIPTION_IDENTIFIER, identValue.getTextValue());
    }

    @Test
    public void testUpdateDescription() throws Exception {
        // Update description for c2
        // C2 initially has one description, and should still have one afterwards
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("description", "c2", "cd2"))
                .entity(descriptionTestStr).put(ClientResponse.class);
        assertStatus(OK, response);

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
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("description", "c2", "cd2"))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateDeleteAccessPoints() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("description", "c2", "cd2", Entities.UNDETERMINED_RELATIONSHIP))
                .entity(accessPointTestStr).post(ClientResponse.class);
        assertStatus(CREATED, response);
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode idNode = rootNode
                .path(Bundle.ID_KEY);
        assertFalse(idNode.isMissingNode());
        String value = idNode.asText();

        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri("description", "c2", "cd2",
                        Entities.UNDETERMINED_RELATIONSHIP, value))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }
}
