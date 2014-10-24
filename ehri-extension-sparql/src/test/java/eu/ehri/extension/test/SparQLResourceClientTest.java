package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.SparQLResource;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import java.net.URI;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test for the SparQL endpoint
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SparQLResourceClientTest extends BaseRestClientTest {

    @Test
    public void testSparQLQuery() throws Exception {
        String testQuery = readResourceFileAsString("testquery.sparql");
        URI queryUri = ehriUriBuilder("sparql").queryParam(SparQLResource.QUERY_PARAM, testQuery).build();
        ClientResponse response = callAs(getAdminUserProfileId(), queryUri).get(ClientResponse.class);
        assertStatus(OK, response);
        String data = response.getEntity(String.class);
        JsonNode rootNode = jsonMapper.readValue(data, JsonNode.class);
        assertTrue(rootNode.isArray());
        JsonNode firstObject = rootNode.path(0);
        assertFalse(firstObject.isMissingNode());
        // NB: This relies on the ORDER by clause and linda being
        // the top name in ascending order - will break if someone
        // changes the fixtures.
        assertEquals("linda", firstObject.path("id").asText());
    }
}
