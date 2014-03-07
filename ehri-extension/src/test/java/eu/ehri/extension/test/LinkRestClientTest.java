package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;

public class LinkRestClientTest extends BaseRestClientTest {

    @Test
    public void testGetLinks() throws Exception {
        // Fetch annotations for an item.
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.LINK, "for", "c1"))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testDeleteAccessPoint() throws Exception {
        // Create a link annotation between two objects
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.LINK, "accessPoint", "ur1"))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateLink() throws Exception {
        // Create a link annotation between two objects
        String jsonLinkTestString = "{\"type\": \"link\", \"data\":{\"identifier\": \"39dj28dhs\", " +
                "\"body\": \"test\", \"type\": \"associate\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.LINK, "c1", "c4")).entity(jsonLinkTestString)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);
    }
}
