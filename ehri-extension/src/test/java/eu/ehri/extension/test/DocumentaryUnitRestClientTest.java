package eu.ehri.extension.test;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DocumentaryUnitRestClientTest extends BaseRestClientTest {

    private String jsonDocumentaryUnitTestStr;
    private String invalidJsonDocumentaryUnitTestStr;
    private String partialJsonDocumentaryUnitTestStr;
    static final String UPDATED_NAME = "UpdatedNameTEST";
    static final String PARTIAL_NAME = "PatchNameTest";
    static final String TEST_JSON_IDENTIFIER = "c1";
    static final String FIRST_DOC_ID = "c1";
    static final String TEST_HOLDER_IDENTIFIER = "r1";
    // FIXME: This ID is temporaty and will break when we decide on a proper
    // prefix ID scheme
    static final String CREATED_ID = "some-id";

    @Before
    public void setUp() throws Exception {
        jsonDocumentaryUnitTestStr = readResourceFileAsString("documentaryUnit.json");
        invalidJsonDocumentaryUnitTestStr = readResourceFileAsString("invalidDocumentaryUnit.json");
        partialJsonDocumentaryUnitTestStr = readResourceFileAsString("partialDocumentaryUnit.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteDocumentaryUnit() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testNotFoundWithValidUrl() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, "r1"))
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testCacheControl() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1"))
                .get(ClientResponse.class);
        assertStatus(OK, response);
        String c1cc = response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        assertThat(c1cc, containsString("no-cache"));
        assertThat(c1cc, containsString("no-store"));
        // C4 is unrestricted and thus has a max-age set
        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, "c4"))
                .get(ClientResponse.class);
        assertStatus(OK, response);
        String c4cc = response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        assertThat(c4cc, not(containsString("no-cache")));
        assertThat(c4cc, not(containsString("no-store")));
        assertThat(c4cc, containsString("max-age=" + AbstractRestResource.ITEM_CACHE_TIME));
    }

    @Test
    public void testCreateDeleteChildDocumentaryUnit() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, FIRST_DOC_ID, Entities.DOCUMENTARY_UNIT))
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testIntegrityError() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Okay... now if we try and do the same things again we should
        // get an integrity error because the identifiers are the same.
        response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);
        // Check the JSON gives use the correct error
        String errString = response.getEntity(String.class);

        assertStatus(BAD_REQUEST, response);

        JsonNode rootNode = jsonMapper.readValue(errString, JsonNode.class);
        JsonNode errValue = rootNode.path(ErrorSet.ERROR_KEY).path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
    }

    @Test
    public void testValidationError() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(invalidJsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        String errorJson = response.getEntity(String.class);
        assertStatus(BAD_REQUEST, response);

        // Check the JSON gives use the correct error
        // In this case the start and end dates for the
        // first date relation should be missing
        JsonNode rootNode = jsonMapper.readValue(errorJson, JsonNode.class);
        JsonNode errValue1 = rootNode.path(ErrorSet.REL_KEY)
                .path(Ontology.DESCRIPTION_FOR_ENTITY).path(0)
                .path(ErrorSet.ERROR_KEY).path(Ontology.NAME_KEY);
        assertFalse(errValue1.isMissingNode());
    }

    @Test
    public void testGetDocumentaryUnitByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, TEST_JSON_IDENTIFIER))
                .get(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(TEST_JSON_IDENTIFIER, errValue.getTextValue());
    }

    @Test
    public void testUpdateDocumentaryUnitByIdentifier() throws Exception {
        // Update doc unit c1 with the test json values, which should change
        // its identifier to some-id
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.DOCUMENTARY_UNIT, TEST_JSON_IDENTIFIER))
                .entity(jsonDocumentaryUnitTestStr)
                .put(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.getTextValue());
    }

    @Test
    public void testListDocumentaryUnit() throws Exception {
        MultivaluedMap<String, String> params = new StringKeyIgnoreCaseMultivaluedMap<String>();
        params.add(AbstractRestResource.SORT_PARAM, Ontology.IDENTIFIER_KEY);
        List<Map<String, Object>> data = getEntityList(
                Entities.DOCUMENTARY_UNIT, getAdminUserProfileId(), params);
        assertTrue(data.size() > 0);
        Collections.sort(data, dataSort);
        // Extract the first documentary unit. According to the fixtures this
        // should be named 'c1'.
        @SuppressWarnings("unchecked")
        Map<String, Object> c1data = (Map<String, Object>) data.get(0).get(
                "data");
        assertEquals(FIRST_DOC_ID, c1data.get(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testListDocumentaryUnitWithOffset() throws Exception {
        // Fetch the second and third doc unit items (c2 and c3)
        MultivaluedMap<String, String> params = new StringKeyIgnoreCaseMultivaluedMap<String>();
        params.add(AbstractRestResource.OFFSET_PARAM, "1");
        params.add(AbstractRestResource.LIMIT_PARAM, "1");
        params.add(AbstractRestResource.SORT_PARAM, Ontology.IDENTIFIER_KEY);
        List<Map<String, Object>> data = getEntityList(
                Entities.DOCUMENTARY_UNIT, getAdminUserProfileId(), params);
        assertEquals(1, data.size());
        Collections.sort(data, dataSort);
        // Extract the second documentary unit. According to the fixtures this
        // should be named 'c2'.
        @SuppressWarnings("unchecked")
        Map<String, Object> c2data = (Map<String, Object>) data.get(0).get("data");
        assertEquals("c2", c2data.get(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testCountDocumentaryUnits() throws Exception {
        Long data = getEntityCount(
                Entities.DOCUMENTARY_UNIT, getAdminUserProfileId());
        assertEquals(Long.valueOf(4), data);
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

        assertStatus(CREATED, response);
        assertValidJsonData(response);
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and change it
        String json = response.getEntity(String.class);
        String toUpdateJson = Bundle.fromString(json)
                .withDataValue("name", UPDATED_NAME).toJson();

        // -update
        resource = client.resource(ehriUri(Entities.DOCUMENTARY_UNIT));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testPatchDocumentaryUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        String toUpdateJson = partialJsonDocumentaryUnitTestStr;

        // - patch the data (using the Patch header)
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header(AbstractRestResource.PATCH_HEADER_NAME, Boolean.TRUE.toString())
                .entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it patched?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                    .get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(CREATED_ID, updatedEntityBundle.getDataValue(Ontology.IDENTIFIER_KEY));
        assertEquals(PARTIAL_NAME, updatedEntityBundle.getDataValue(Ontology.NAME_KEY));
    }

    private URI getCreationUri() {
        return ehriUri(Entities.REPOSITORY, TEST_HOLDER_IDENTIFIER, Entities.DOCUMENTARY_UNIT);
    }

    private Comparator<Map<String, Object>> dataSort = new Comparator<Map<String, Object>>() {
        @Override
        public int compare(Map<String, Object> a, Map<String, Object> b) {
            return ((String) a.get("id")).compareTo((String) b.get("id"));
        }
    };
}
