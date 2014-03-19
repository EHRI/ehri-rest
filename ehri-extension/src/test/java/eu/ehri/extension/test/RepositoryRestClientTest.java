package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RepositoryRestClientTest extends BaseRestClientTest {

    static final String COUNTRY_CODE = "nl";
    static final String ID = "r1";
    static final String LIMITED_USER_NAME = "reto";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String agentTestData;
    private String docTestData;

    @Before
    public void setUp() throws Exception {
        agentTestData = readFileAsString("repository.json");
        docTestData = readFileAsString("documentaryUnit.json");
    }

    @Test
    public void testCreateRepository() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(agentTestData)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateRepositoryWithExistingIdentifier() throws Exception {
        String json = Bundle.fromString(agentTestData)
                .withDataValue(Ontology.IDENTIFIER_KEY, "r1").toJson();
        URI uri = ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                uri).entity(json)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Now do it again!
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(json)
                .post(ClientResponse.class);
        assertStatus(BAD_REQUEST, response);
        System.out.println(response.getEntity(String.class));
    }

    @Test
    public void testUpdateRepositoryByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(agentTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Obtain some update data.
        String updateData = Bundle.fromString(agentTestData)
                .withDataValue("name", UPDATED_NAME).toJson();

        response = jsonCallAs(getAdminUserProfileId(),
                response.getLocation()).entity(updateData)
                .put(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateRepositoryWithDeserializationError() throws Exception {
        // Create
        String badRepositoryTestData = "{\"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(badRepositoryTestData)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);

        // Check the JSON gives use the correct error
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("error");
        assertFalse(errValue.isMissingNode());
        assertEquals(DeserializationError.class.getSimpleName(),
                errValue.asText());
    }

    @Test
    public void testDeleteRepository() throws Exception {
        // Create
        URI uri = ehriUri(Entities.REPOSITORY, ID);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                uri)
                .delete(ClientResponse.class);

        assertStatus(OK, response);

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);

        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testGrantPermsForRepositoryScope() throws Exception {
        // Grant permissions for a user to create items within this scope.

        // The user shouldn't be able to create docs with r2
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                getCreationUriFor("r2")).entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(UNAUTHORIZED, response);

        // Or r3...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r3"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(UNAUTHORIZED, response);

        // Now grant the user permissions to create just within
        // the scope of r2
        String permData = "{\"documentaryUnit\": [\"create\"]}";

        URI grantUri = ehriUri(Entities.PERMISSION, LIMITED_USER_NAME, "scope", "r2");

        response = jsonCallAs(getAdminUserProfileId(), grantUri)
                .entity(permData)
                .post(ClientResponse.class);

        assertStatus(OK, response);

        // Now creation should succeed...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r2"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // But r3 should still fail...
        // Or r3...
        response = jsonCallAs(LIMITED_USER_NAME, getCreationUriFor("r3"))
                .entity(docTestData)
                .post(ClientResponse.class);
        assertStatus(UNAUTHORIZED, response);

        // And the user himself should not be able to grant
        // others the ability to create within that scope.
        String otherUserName = "linda";
        String grantPermData = "{\"documentaryUnit\": [\"grant\"]}";
        URI otherGrantUri = ehriUri(Entities.PERMISSION, otherUserName, "scope", "r2");

        response = jsonCallAs(LIMITED_USER_NAME, otherGrantUri)
                .entity(grantPermData)
                .post(ClientResponse.class);

        assertStatus(UNAUTHORIZED, response);

    }

    private URI getCreationUriFor(String id) {
        return ehriUri(Entities.REPOSITORY, id, Entities.DOCUMENTARY_UNIT);
    }

}
