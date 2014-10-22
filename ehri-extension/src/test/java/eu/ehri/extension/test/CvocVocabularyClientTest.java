package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;


public class CvocVocabularyClientTest extends BaseRestClientTest {
    static final String TEST_CVOC_ID = "cvoc1"; // vocabulary in fixture

    private String jsonTestVocabularyString = "{\"type\":\"" + Entities.CVOC_VOCABULARY +
            "\",\"data\":{\"identifier\": \"plants\", \"name\": \"Plants\"}}";
    private String jsonApplesTestStr;

    @Before
    public void setUp() throws Exception {
        jsonApplesTestStr = readFileAsString("apples.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteCvocVocabulary() throws Exception {
        String jsonTestString = jsonTestVocabularyString;

        // Create
        ClientResponse response = testCreate(ehriUri(Entities.CVOC_VOCABULARY),
                jsonTestString);

        // Get created entity via the response location?
        URI location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(OK, response);
    }

    // Test adding a concept, 
    // more concept testing is done elsewere
    @Test
    public void testCreateCvocConcept() throws Exception {
        // Create
        ClientResponse response = testCreate(ehriUri(Entities.CVOC_VOCABULARY),
                jsonTestVocabularyString);

        // Get created entity via the response location?
        URI location = response.getLocation();
        //... ehh get the id  ... we need to fix this!
        String locationStr = location.getPath();
        String vocIdStr = locationStr.substring(locationStr.lastIndexOf('/') + 1);

        response = testCreate(
                ehriUri(Entities.CVOC_VOCABULARY, vocIdStr, Entities.CVOC_CONCEPT),
                jsonApplesTestStr);
        location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(OK, response);
    }

    @Test
    public void testGetVocabulary() throws Exception {
        testGet(ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID));
    }

    @Test
    public void testDeleteVocabularyTerms() throws Exception {
        URI uri = ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID, "all");
        testDelete(uri);

        // Check it's really gone...
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.CVOC_CONCEPT, "cvocc1"))
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testDeleteVocabulary() throws Exception {
        URI uri = ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID);
        testDelete(uri);

        // Check it's really gone...
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    /**
     * helpers **
     */

    // Note: maybe generalize them and reuse for other tests
    public ClientResponse testGet(URI uri) {
        return jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
    }

    public ClientResponse testCreate(URI uri, String json) {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(json).post(ClientResponse.class);
        assertStatus(CREATED, response);
        return response;
    }

    public ClientResponse testDelete(URI uri) {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(OK, response);

        return response;
    }
}
