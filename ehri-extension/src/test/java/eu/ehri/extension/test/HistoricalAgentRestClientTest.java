package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HistoricalAgentRestClientTest extends BaseRestClientTest {

    static final String TEST_ID = "a1";
    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String authorityTestData;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(HistoricalAgentRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        authorityTestData = readFileAsString("historicalAgent.json");
    }

    @Test
    public void testCreateAuthority() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.HISTORICAL_AGENT);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(authorityTestData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateAuthorityWithExistingIdentifier() throws Exception {
        String json = Bundle.fromString(authorityTestData)
                .withDataValue(Ontology.IDENTIFIER_KEY, "r1").toJson();
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.HISTORICAL_AGENT);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(json)
                .post(ClientResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        System.out.println(response.getEntity(String.class));
    }

    @Test
    public void testUpdateAuthorityByIdentifier() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.HISTORICAL_AGENT);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(authorityTestData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Obtain some update data.
        String updateData = Bundle.fromString(authorityTestData)
                .withDataValue("name", UPDATED_NAME).toJson();

        resource = client.resource(response.getLocation());
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(updateData)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateAuthorityWithDeserializationError() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.HISTORICAL_AGENT);
        String badAuthorityTestData = "{\"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(badAuthorityTestData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

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
    public void testDeleteAuthority() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.HISTORICAL_AGENT + "/" + TEST_ID);
        ClientResponse response = resource.header(
                AbstractRestResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .delete(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Check it's really gone...
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                response.getStatus());
    }
}
