package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;

public class AnnotationRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonAnnotationTestString = "{\"type\": \"annotation\", \"data\":{\"identifier\": \"39dj28dhs\", \"body\": \"test\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AnnotationRestClientTest.class.getName());
    }

    @Test
    public void testGetAnnotations() throws Exception {
        // Fetch annotations for an item.
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/annotation/for/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        System.out.println(response.getEntity(String.class));
        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());        
    }

    @Test
    public void testCreateAnnotation() throws Exception {
        // Create a link annotation between two objects
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/annotation/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAnnotationTestString)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());        
    }
    
    @Test
    public void testCreateLinkAnnotation() throws Exception {
        // Create a link annotation between two objects
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/annotation/c1/r1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAnnotationTestString)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());        
    }
}
