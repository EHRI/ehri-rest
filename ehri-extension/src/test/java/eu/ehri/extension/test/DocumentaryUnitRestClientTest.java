package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Bundle;

public class DocumentaryUnitRestClientTest extends BaseRestClientTest {

    private String jsonDocumentaryUnitTestStr; // test data to create a
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
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        // Check the JSON gives use the correct error
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("details").path("fields")
                .path(AccessibleEntity.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.getTextValue());
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
                EntityTypes.DOCUMENTARY_UNIT, getAdminUserProfileId());
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
        Bundle<DocumentaryUnit> entityBundle = converter
                .jsonToBundle(json);
        entityBundle.setDataValue("name", UPDATED_NAME);
        String toUpdateJson = converter.bundleToJson(entityBundle);

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
        Bundle<DocumentaryUnit> updatedEntityBundle = converter
                .jsonToBundle(updatedJson);
        Map<String, Object> updatedData = updatedEntityBundle.getData();
        assertEquals(UPDATED_NAME, updatedData.get("name"));
    }

    private URI getCreationUri() {
        return UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(EntityTypes.AGENT).segment(TEST_HOLDER_IDENTIFIER)
                .segment(EntityTypes.DOCUMENTARY_UNIT).build();
    }
}
