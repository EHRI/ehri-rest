package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HistoricalAgentRestClientTest extends BaseRestClientTest {

    static final String TEST_ID = "a1";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String authorityTestData;

    @Before
    public void setUp() throws Exception {
        authorityTestData = readFileAsString("historicalAgent.json");
    }

    @Test
    public void testCreateAuthority() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.HISTORICAL_AGENT))
                .entity(authorityTestData)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), location).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateAuthorityWithExistingIdentifier() throws Exception {
        String json = Bundle.fromString(authorityTestData)
                .withDataValue(Ontology.IDENTIFIER_KEY, "r1").toJson();

        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.HISTORICAL_AGENT)).entity(json)
                .post(ClientResponse.class);
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testUpdateAuthorityByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.HISTORICAL_AGENT)).entity(authorityTestData)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);

        // Obtain some update data.
        String updateData = Bundle.fromString(authorityTestData)
                .withDataValue("name", UPDATED_NAME).toJson();

        response = jsonCallAs(getAdminUserProfileId(), response.getLocation())
                .entity(updateData)
                .put(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateAuthorityWithDeserializationError() throws Exception {
        // Create
        String badAuthorityTestData = "{\"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.HISTORICAL_AGENT)).entity(badAuthorityTestData)
                .post(ClientResponse.class);

        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testDeleteAuthority() throws Exception {
        // Create
        URI uri = ehriUri(Entities.HISTORICAL_AGENT, TEST_ID);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                uri)
                .delete(ClientResponse.class);

        assertStatus(OK, response);

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(), uri).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }
}
