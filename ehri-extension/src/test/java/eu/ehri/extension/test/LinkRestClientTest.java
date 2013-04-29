package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class LinkRestClientTest extends BaseRestClientTest {

    static final String UPDATED_NAME = "UpdatedNameTEST";

    private String jsonLinkTestString = "{\"type\": \"link\", \"data\":{\"identifier\": \"39dj28dhs\", " +
            "\"body\": \"test\", \"type\": \"associate\"}}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(LinkRestClientTest.class.getName());
    }

    @Test
    public void testGetLinks() throws Exception {
        // Fetch annotations for an item.
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.LINK + "/for/c1");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());        
    }

    @Test
    public void testCreateLink() throws Exception {
        // Create a link annotation between two objects
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.LINK + "/c1/c4");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonLinkTestString)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());        
    }
}
