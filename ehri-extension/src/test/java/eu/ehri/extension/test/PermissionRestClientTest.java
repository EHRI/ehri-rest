package eu.ehri.extension.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.models.EntityTypes;

/**
 * Test Permissions resource.
 * 
 * FIXME: Remove lots of
 * 
 * @author michaelb
 * 
 */
public class PermissionRestClientTest extends BaseRestClientTest {

    static final String LIMITED_USER_NAME = "reto";
    static final String TEST_HOLDER_IDENTIFIER = "r2";

    private String jsonDocumentaryUnitTestStr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(PermissionRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        jsonDocumentaryUnitTestStr = readFileAsString("documentaryUnit.json");
    }

    @Test
    public void testSettingGlobalPermissionMatrix()
            throws JsonGenerationException, JsonMappingException,
            UniformInterfaceException, IOException {

        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE
                + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<Map<String, Map<String, List<String>>>> currentMatrix = getInheritedMatrix(response
                .getEntity(String.class));
        // Check we don't ALREADY have documentaryUnit -> create/delete perms
        assertNull(currentMatrix.get(0).get(LIMITED_USER_NAME)
                .get(EntityTypes.DOCUMENTARY_UNIT));
        assertNull(currentMatrix.get(0).get(LIMITED_USER_NAME)
                .get(EntityTypes.DOCUMENTARY_UNIT));

        // Set the permission via REST
        resource = client.resource(getExtensionEntryPointUri() + "/"
                + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE + "/"
                + LIMITED_USER_NAME);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(new ObjectMapper().writeValueAsBytes(getTestMatrix()))
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Retry the create action
        resource = client.resource(getExtensionEntryPointUri() + "/"
                + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE + "/"
                + LIMITED_USER_NAME);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<Map<String, Map<String, List<String>>>> newMatrix = getInheritedMatrix(response
                .getEntity(String.class));

        // Check we don't ALREADY have documentaryUnit -> create/delete perms
        assertTrue(newMatrix.get(0).get(LIMITED_USER_NAME)
                .get(EntityTypes.DOCUMENTARY_UNIT)
                .contains(PermissionTypes.CREATE));
        assertTrue(newMatrix.get(0).get(LIMITED_USER_NAME)
                .get(EntityTypes.DOCUMENTARY_UNIT)
                .contains(PermissionTypes.DELETE));
    }

    @Test
    public void testSettingGlobalPermissions() throws JsonGenerationException,
            JsonMappingException, UniformInterfaceException, IOException {

        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Set the permission via REST
        resource = client.resource(getExtensionEntryPointUri() + "/"
                + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE + "/"
                + LIMITED_USER_NAME);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(new ObjectMapper().writeValueAsBytes(getTestMatrix()))
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Retry the create action
        resource = client.resource(getCreationUri());
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        // Should get CREATED this time...
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Finally, delete the item
        URI loc = response.getLocation();
        resource = client.resource(response.getLocation());
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).delete(ClientResponse.class);

        // Should get CREATED this time...
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private List<Map<String, Map<String, List<String>>>> getInheritedMatrix(
            String json) throws JsonParseException, JsonMappingException,
            IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<LinkedList<HashMap<String, Map<String, List<String>>>>> typeRef = new TypeReference<LinkedList<HashMap<String, Map<String, List<String>>>>>() {
        };
        return mapper.readValue(json, typeRef);
    }

    @SuppressWarnings("serial")
    private Map<String, List<String>> getTestMatrix() {
        // @formatter:off
        Map<String,List<String>> matrix = new HashMap<String, List<String>>() {{
            put(EntityTypes.DOCUMENTARY_UNIT, new LinkedList<String>() {{
                add(PermissionTypes.CREATE);
                add(PermissionTypes.DELETE);
                add(PermissionTypes.UPDATE);
            }});
            put(EntityTypes.AGENT, new LinkedList<String>() {{
                add(PermissionTypes.CREATE);
                add(PermissionTypes.DELETE);
                add(PermissionTypes.UPDATE);
            }});
        }};
        // @formatter:on
        return matrix;
    }

    private URI getCreationUri() {
        return UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(EntityTypes.AGENT).segment(TEST_HOLDER_IDENTIFIER)
                .segment(EntityTypes.DOCUMENTARY_UNIT).build();
    }
}
