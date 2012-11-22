package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;

public class CvocConceptClientTest  extends BaseRestClientTest {

    private String jsonCvocConceptTestString = "{\"data\":{\"identifier\": \"apples\",\"isA\":\"cvocConcept\"}}";
    private String jsonApplesTestStr;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(CvocConceptClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        jsonApplesTestStr = readFileAsString("apples.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteCvocConcept() throws Exception {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/cvocConcept");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonApplesTestStr/*jsonCvocConceptTestString*/).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get created entity via the response location?
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
    public void testRelatedCvocConcepts() throws Exception {
        String jsonFruitTestStr =  "{\"data\":{\"identifier\": \"fruit\",\"isA\":\"cvocConcept\"}}";
        //readFileAsString("fruit.json");
        String jsonAppleTestStr =  "{\"data\":{\"identifier\": \"apple\",\"isA\":\"cvocConcept\"}}";
        
        ClientResponse response;
        
        // Create fruit
        response = testCreateConcept(jsonFruitTestStr);
        // Get created entity via the response location
        URI fruitLocation = response.getLocation();
       
        // Create apple
        response = testCreateConcept(jsonAppleTestStr);
        // Get created entity via the response location
        URI appleLocation = response.getLocation();
        //... ehh get the id number ... we need to fix this!
        String appleLocationStr = appleLocation.getPath();
        String appleIdStr = appleLocationStr.substring(appleLocationStr.lastIndexOf('/') + 1);
        
        // make apple narrower of fruit
        WebResource resource = client.resource(fruitLocation + "/narrower/" + appleIdStr);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity("").post(ClientResponse.class);
        // Hmm, it's a post request, but we don't create a vertex (but we do an edge...)
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
//        // get fruit's narrower concepts
//        resource = client.resource(fruitLocation + "/narrower/list");
//        response = resource
//                .accept(MediaType.APPLICATION_JSON)
//                .header(AbstractRestResource.AUTH_HEADER_NAME,
//                        getAdminUserProfileId()).get(ClientResponse.class);
//        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
//        String json = response.getEntity(String.class);
//        
//        ObjectMapper mapper = new ObjectMapper();
//        //List list = mapper.readValue(response.getEntity(String.class), List.class);
//        JsonNode rootNode = mapper.readValue(json, JsonNode.class);
//System.out.println("narrower list Response json string: " + json);
        
//        // ehh, is apple in there as a concept?
//        resource = client.resource(getExtensionEntryPointUri()
//                + "/cvocConcept/apple");
//        response = resource
//                .accept(MediaType.APPLICATION_JSON)
//                .header(AbstractRestResource.AUTH_HEADER_NAME,
//                        getAdminUserProfileId()).get(ClientResponse.class);
//        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());       
        
        
//        Iterator<JsonNode> elements = rootNode.getElements();
//        while(elements.hasNext()) {
//        	JsonNode node = elements.next();
//        	System.out.println(node);
//        }
    }
    
    public ClientResponse testCreateConcept(String json) {
        // Create
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/cvocConcept");
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
    
    /**
     * Get the id of a value from its JSON string representation.
     * 
     * @param responseStr
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    private Long getIdFromResponseString(String responseStr)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(responseStr, Map.class);
        String self = (String) data.get("self");
        return Long.valueOf(self.substring(self.lastIndexOf('/') + 1));
    }
}
