package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;

public class CvocVocabularyClientTest extends BaseRestClientTest {
    static final String TEST_CVOC_ID = "cvoc1"; // vocabulary in fixture

    private String jsonTestVocabularyString = "{\"type\":\"" + Entities.CVOC_VOCABULARY + 
			"\",\"data\":{\"identifier\": \"plants\"}}";
    private String jsonApplesTestStr;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(CvocVocabularyClientTest.class.getName());
    }

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
        ClientResponse response = testCreate(Entities.CVOC_VOCABULARY, jsonTestString);
        //System.out.println("POST Respons json: " + response.getEntity(String.class));
        
        // Get created entity via the response location?
        URI location = response.getLocation();

        response = testGet(location.toString());
        // TODO again test json
        //System.out.println("GET Respons json: " + response.getEntity(String.class));

        response = testDelete(location.toString());
    }

    // Test adding a concept, 
    // more concept testing is done elsewere
    @Test
    public void testCreateCvocConcept() throws Exception {    	
    	// Create
        ClientResponse response = testCreate(Entities.CVOC_VOCABULARY, jsonTestVocabularyString);
        //System.out.println("POST Respons json: " + response.getEntity(String.class));
        
        // Get created entity via the response location?
        URI location = response.getLocation();
        //... ehh get the id  ... we need to fix this!
        String locationStr = location.getPath();
        String vocIdStr = locationStr.substring(locationStr.lastIndexOf('/') + 1);

        response = testCreate(Entities.CVOC_VOCABULARY + "/" + vocIdStr + "/" + Entities.CVOC_CONCEPT, jsonApplesTestStr);
        location = response.getLocation();
        
        response = testGet(location.toString());
        //System.out.println("GET Respons json: " + response.getEntity(String.class));
        
        response = testDelete(location.toString());
    }
    
    @Test
    public void testGetVocabulary() throws Exception {    	
    	testGet(getExtensionEntryPointUri()
    			+ "/"  + Entities.CVOC_VOCABULARY + "/" + TEST_CVOC_ID);
    }

    @Test
    public void testDeleteVocabulary() throws Exception {
    	String url = getExtensionEntryPointUri()
    			+ "/"  + Entities.CVOC_VOCABULARY + "/" + TEST_CVOC_ID;
    	testDelete(url);
    	
        // Check it's really gone...
    	WebResource resource = client.resource(url);
        ClientResponse responseDel = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
        		responseDel.getStatus());
    }
    
    /*** helpers ***/
    
    // Note: maybe generalize them and reuse for other tests
    
    public ClientResponse testGet(String url) {
    	WebResource resource = client.resource(url);
    	ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
     	
        return response;
    }
    
    public ClientResponse testCreate(String relUri, String json) {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + relUri);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(json).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
    	
        return response;
    }
    
    public ClientResponse testDelete(String url) {
    	WebResource resource = client.resource(url);
    	ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        return response;
    }
    

}
