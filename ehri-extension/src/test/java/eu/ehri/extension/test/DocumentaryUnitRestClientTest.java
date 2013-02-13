package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import eu.ehri.project.models.base.DescribedEntity;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.BundleError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.Bundle;

public class DocumentaryUnitRestClientTest extends BaseRestClientTest {

    private String jsonDocumentaryUnitTestStr;
    private String invalidJsonDocumentaryUnitTestStr;
    static final String UPDATED_NAME = "UpdatedNameTEST";
    static final String TEST_JSON_IDENTIFIER = "c1";
    static final String FIRST_DOC_ID = "c1";
    static final String TEST_HOLDER_IDENTIFIER = "r1";
    // FIXME: This ID is temporaty and will break when we decide on a proper
    // prefix ID scheme
    static final String CREATED_ID = "some-id";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(DocumentaryUnitRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        jsonDocumentaryUnitTestStr = readFileAsString("documentaryUnit.json");
        invalidJsonDocumentaryUnitTestStr = readFileAsString("invalidDocumentaryUnit.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteDocumentaryUnit() throws Exception {
        // Create
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)
        // System.out.println("POST Respons json: " +
        // response.getEntity(String.class));

        // Get created doc via the response location?
        URI location = response.getLocation();
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
    }

    @Test
    public void testIntegrityError() throws Exception {
        // Create
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Okay... now if we try and do the same things again we should
        // get an integrity error because the identifiers are the same.
        resource = client.resource(getCreationUri());
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);
        // Check the JSON gives use the correct error
        String errString = response.getEntity(String.class);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(errString, JsonNode.class);
        JsonNode errValue = rootNode.path(BundleError.ERROR_KEY).path(
                AccessibleEntity.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
    }

    @Test
    public void testValidationError() throws Exception {
        // Create
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(invalidJsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        String errorJson = response.getEntity(String.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        // Check the JSON gives use the correct error
        // In this case the start and end dates for the
        // first date relation should be missing
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(errorJson, JsonNode.class);
        JsonNode errValue1 = rootNode.path(BundleError.REL_KEY)
                .path(DescribedEntity.DESCRIBES).path(0)
                .path(BundleError.REL_KEY)
                .path(TemporalEntity.HAS_DATE).path(0)
                .path(BundleError.ERROR_KEY).path(DatePeriod.START_DATE);
        assertFalse(errValue1.isMissingNode());
    }

    @Test
    public void testGetDocumentaryUnitByIdentifier() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/" + TEST_JSON_IDENTIFIER);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                AccessibleEntity.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(TEST_JSON_IDENTIFIER, errValue.getTextValue());
    }

    @Test
    public void testUpdateDocumentaryUnitByIdentifier() throws Exception {
        // Update doc unit c1 with the test json values, which should change
        // its identifier to some-id
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/" + TEST_JSON_IDENTIFIER);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(jsonDocumentaryUnitTestStr)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).put(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                AccessibleEntity.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.getTextValue());
    }

    @Test
    public void testListDocumentaryUnit() throws Exception {
        List<Map<String, Object>> data = getEntityList(
                Entities.DOCUMENTARY_UNIT, getAdminUserProfileId());
        assertTrue(data.size() > 0);
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> c1, Map<String, Object> c2) {
                return ((String) c1.get("id")).compareTo((String) c2.get("id"));
            }
        });
        // Extract the first documentary unit. According to the fixtures this
        // should be named 'c1'.
        @SuppressWarnings("unchecked")
        Map<String, Object> c1data = (Map<String, Object>) data.get(0).get(
                "data");
        assertEquals(FIRST_DOC_ID, c1data.get(AccessibleEntity.IDENTIFIER_KEY));
    }

    @Test
    public void testUpdateDocumentaryUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and change it
        String json = response.getEntity(String.class);
        String toUpdateJson = Bundle.fromString(json)
                .withDataValue("name", UPDATED_NAME).toJson();

        // -update
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    private URI getCreationUri() {
        return UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.AGENT).segment(TEST_HOLDER_IDENTIFIER)
                .segment(Entities.DOCUMENTARY_UNIT).build();
    }
}
