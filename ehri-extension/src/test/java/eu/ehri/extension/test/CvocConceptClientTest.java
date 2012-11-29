package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

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
	 
    //private String jsonCvocConceptTestString = "{\"data\":{\"identifier\": \"apples\",\"isA\":\"cvocConcept\"}}";
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
                .entity(jsonApplesTestStr).post(ClientResponse.class);

        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());
        //System.out.println("POST Respons json: " + response.getEntity(String.class));

        // Get created entity via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
        //System.out.println("GET Respons json: " + response.getEntity(String.class));
        
        // Where is my deletion test, I want to know if it works
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
// NOTE FIX this, now we get an java.lang.IllegalArgumentException: Element can not be null
//        response = resource
//                .accept(MediaType.APPLICATION_JSON)
//                .header(AbstractRestResource.AUTH_HEADER_NAME,
//                        getAdminUserProfileId()).get(ClientResponse.class);
//        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        
    }

    /**
     * Add and remove narrower concept
     * 
     * @throws Exception
     */
    @Test
    public void testNarrowerCvocConcepts() throws Exception {
        String jsonFruitTestStr =  "{\"data\":{\"identifier\": \"fruit\",\"isA\":\"cvocConcept\"}}";
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
                .post(ClientResponse.class);
        // Hmm, it's a post request, but we don't create a vertex (but we do an edge...)
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());        
        //System.out.println("add narrower Respons json: " + response.getEntity(String.class));

        // get fruit's narrower concepts
        response = testGet(fruitLocation + "/narrower/list");        
        // check if apple is in there
        assertTrue(containsIdentifier(response, "apple"));
        
        // check if apple's broader is fruit
        // get apple's broader concepts
        response = testGet(getExtensionEntryPointUri()
        					+ "/cvocConcept/" + appleIdStr + "/broader/list");        
        // check if fruit is in there
        assertTrue(containsIdentifier(response, "fruit"));
        
        // Test removal of one narrower concept
        resource = client.resource(fruitLocation + "/narrower/" + appleIdStr);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());        
        
        // apple should still exist
        response = testGet(getExtensionEntryPointUri()
        					+ "/cvocConcept/" + appleIdStr);
        
        // but not as a narrower of fruit!
        response = testGet(fruitLocation + "/narrower/list");  
        // check if apple is NOT in there
        assertFalse(containsIdentifier(response, "apple"));
    }

    /**
     * Add and remove related concept
     * Similar to the narrower/broader 'relationships'
     * 
     * @throws Exception
     */
    @Test
    public void testRelatedCvocConcepts() throws Exception {
        String jsonTreeTestStr =  "{\"data\":{\"identifier\": \"tree\",\"isA\":\"cvocConcept\"}}";
        String jsonAppleTestStr =  "{\"data\":{\"identifier\": \"apple\",\"isA\":\"cvocConcept\"}}";
        
        ClientResponse response;
        
        // Create fruit
        response = testCreateConcept(jsonTreeTestStr);
        // Get created entity via the response location
        URI treeLocation = response.getLocation();
       
        // Create apple
        response = testCreateConcept(jsonAppleTestStr);
        
        // Get created entity via the response location
        URI appleLocation = response.getLocation();
        //... ehh get the id number ... we need to fix this!
        String appleLocationStr = appleLocation.getPath();
        String appleIdStr = appleLocationStr.substring(appleLocationStr.lastIndexOf('/') + 1);
        
        // make apple related of fruit
        WebResource resource = client.resource(treeLocation + "/related/" + appleIdStr);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        // Hmm, it's a post request, but we don't create a vertex (but we do an edge...)
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());        
        //System.out.println("add related Respons json: " + response.getEntity(String.class));

        // get fruit's related concepts
        response = testGet(treeLocation + "/related/list");        
        // check if apple is in there
        assertTrue(containsIdentifier(response, "apple"));
        
        // check if apple's relatedBy is tree
        // get apple's broader concepts
        response = testGet(getExtensionEntryPointUri()
        					+ "/cvocConcept/" + appleIdStr + "/relatedBy/list");        
        // check if tree is in there
        assertTrue(containsIdentifier(response, "tree"));

        
        // Test removal of one related concept
        resource = client.resource(treeLocation + "/related/" + appleIdStr);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .delete(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());        
        
        // apple should still exist
        response = testGet(getExtensionEntryPointUri()
        					+ "/cvocConcept/" + appleIdStr);
        
        // but not as a related of tree!
        response = testGet(treeLocation + "/related/list");  
        // check that apple is NOT in there
        assertFalse(containsIdentifier(response, "apple"));
    }
    
    /*** helpers ***/
    
    public ClientResponse testGet(String url) {
    	WebResource resource = client.resource(url);
    	ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
     	
        return response;
    }
    
    public boolean containsIdentifier(final ClientResponse response, final String idStr) throws JsonParseException, JsonMappingException, IOException {
        String json = response.getEntity(String.class);
        //System.out.println("list Response json: " + json);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(json, JsonNode.class);
        return containsIdentifier(rootNode.getElements(), idStr);
    }
    
    public boolean containsIdentifier(final Iterator<JsonNode> elements, final String idStr) {
		if (idStr == null || elements == null) return false;

		boolean result = false;
    	while (elements.hasNext()) {
			JsonNode node = elements.next();
			// assume only one 'identifier' field under the 'data' element
			JsonNode idNode = node.findValue("identifier");
			if (null==idNode) continue;
			
			if (0==idStr.compareTo(idNode.asText())) {
				result = true;
				break; // found!
			}
		}        
    	return result;
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
}
