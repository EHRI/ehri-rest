package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.EntityBundle;

public class DocumentaryUnitRestClientTest extends BaseRestClientTest {

    private String jsonDocumentaryUnitTestStr; // test data to create a
                                               // DocumentaryUnit
    static final String UPDATED_NAME = "UpdatedNameTEST";
    static final String TEST_JSON_IDENTIFIER = "c1";
    static final String FIRST_DOC_ID = "c1";

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
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
    }

    @Test
    public void testGetDocumentaryUnitBySlug() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/" + TEST_JSON_IDENTIFIER);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String json = response.getEntity(String.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(json, Map.class);
        assertTrue(data.get("data") != null);
        assertEquals(TEST_JSON_IDENTIFIER,
                ((Map<String, Object>) data.get("data"))
                        .get(AccessibleEntity.IDENTIFIER_KEY));
    }

    @Test
    public void testListDocumentaryUnit() throws Exception {
        List<Map<String, Object>> data = getEntityList(
                EntityTypes.DOCUMENTARY_UNIT, getAdminUserProfileId());
        assertTrue(data.size() > 0);
        // Extract the first documentary unit. According to the fixtures this
        // should be named 'c1'.
        Map<String, Object> c1data = (Map<String, Object>) data.get(0).get(
                "data");
        assertEquals(FIRST_DOC_ID, c1data.get(AccessibleEntity.IDENTIFIER_KEY));
    }

    @Test
    public void testUpdateDocumentaryUnit() throws Exception {

        // -create data for testing
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonDocumentaryUnitTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and change it
        String json = response.getEntity(String.class);
        EntityBundle<DocumentaryUnit> entityBundle = converter
                .jsonToBundle(json);
        Map<String, Object> data = entityBundle.getData();
        entityBundle.setDataValue("name", UPDATED_NAME);
        String toUpdateJson = converter.bundleToJson(entityBundle);

        // -update
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        EntityBundle<DocumentaryUnit> updatedEntityBundle = converter
                .jsonToBundle(updatedJson);
        Map<String, Object> updatedData = updatedEntityBundle.getData();
        assertEquals(UPDATED_NAME, updatedData.get("name"));
    }
}
