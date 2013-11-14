package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;

public class AnnotationRestClientTest extends BaseRestClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(AnnotationRestClientTest.class.getName());
    }

    @Test
    public void testGetAnnotations() throws Exception {
        // Fetch annotations for an item.
        WebResource resource = client.resource(ehriUri("annotation", "for", "c1"));
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateAnnotation() throws Exception {
        // Create a link annotation between two objects
        WebResource resource = client.resource(ehriUri("annotation", "c1"));
        String jsonAnnotationTestString = "{\"type\": \"annotation\", " +
                "\"data\":{\"identifier\": \"39dj28dhs\", \"body\": \"test\"}}";
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAnnotationTestString)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);
    }
}
