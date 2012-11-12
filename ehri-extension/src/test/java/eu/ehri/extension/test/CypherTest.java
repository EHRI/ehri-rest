package eu.ehri.extension.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * Test Permissions resource.
 * 
 * FIXME: Remove lots of
 * 
 * @author michaelb
 * 
 */
public class CypherTest extends BaseRestClientTest {

    static final String TEST_HOLDER_IDENTIFIER = "r1";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(CypherTest.class.getName());
    }

    @Test
    public void testSettingGlobalPermissionMatrix()
            throws JsonGenerationException, JsonMappingException,
            UniformInterfaceException, IOException {

        String cypherQuery = "START n = node(*) WHERE has(n.identifier) and n.identifier = '"
                + TEST_HOLDER_IDENTIFIER + "' RETURN n";
        Map<String, Object> cypher = new HashMap<String, Object>();
        cypher.put("query", cypherQuery);
        cypher.put("params", new HashMap<String, Object>());

        WebResource resource = client
                .resource(getBaseUri() + "/db/data/cypher");
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ObjectMapper().writeValueAsBytes(cypher))
                .post(ClientResponse.class);
        System.out.println(response.getEntity(String.class));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
