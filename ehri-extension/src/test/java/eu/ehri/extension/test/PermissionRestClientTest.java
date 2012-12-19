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

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;

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
                + "/" + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
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
                .get(ContentTypes.DOCUMENTARY_UNIT.getName()));
        assertNull(currentMatrix.get(0).get(LIMITED_USER_NAME)
                .get(ContentTypes.DOCUMENTARY_UNIT.getName()));

        // Set the permission via REST
        resource = client.resource(getExtensionEntryPointUri() + "/"
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(new ObjectMapper().writeValueAsBytes(getTestMatrix()))
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        resource = client.resource(getExtensionEntryPointUri() + "/"
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
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
                .get(ContentTypes.DOCUMENTARY_UNIT.getName())
                .contains(PermissionType.CREATE.getName()));
        assertTrue(newMatrix.get(0).get(LIMITED_USER_NAME)
                .get(ContentTypes.DOCUMENTARY_UNIT.getName())
                .contains(PermissionType.DELETE.getName()));
    }

    @Test
    public void testPermissionSetPermissionDenied() throws JsonGenerationException,
            JsonMappingException, UniformInterfaceException, IOException {

        // Test a user setting his own permissions over REST - this should
        // obviously fail...
        WebResource resource = client.resource(getExtensionEntryPointUri() + "/"
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME)
                .entity(new ObjectMapper().writeValueAsBytes(getTestMatrix()))
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testGivingBadPermsErrorsCorrectly() throws JsonGenerationException,
            JsonMappingException, UniformInterfaceException, IOException {

        // If we give a permission matrix for a content type that doesn't
        // exist we should get a DeserializationError in return.
        Map<String, List<String>> testMatrix = getTestMatrix();
        testMatrix.put("IDONTEXIST", new ImmutableList.Builder<String>()
                    .add(PermissionType.CREATE.getName()).build());
        
        // Set the permission via REST
        WebResource resource = client.resource(getExtensionEntryPointUri() + "/"
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(new ObjectMapper().writeValueAsBytes(testMatrix))
                .post(ClientResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
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
            put(ContentTypes.DOCUMENTARY_UNIT.getName(), new LinkedList<String>() {{
                add(PermissionType.CREATE.getName());
                add(PermissionType.DELETE.getName());
                add(PermissionType.UPDATE.getName());
            }});
            put(ContentTypes.AGENT.getName(), new LinkedList<String>() {{
                add(PermissionType.CREATE.getName());
                add(PermissionType.DELETE.getName());
                add(PermissionType.UPDATE.getName());
            }});
        }};
        // @formatter:on
        return matrix;
    }

    private URI getCreationUri() {
        return UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.AGENT).segment(TEST_HOLDER_IDENTIFIER)
                .segment(Entities.DOCUMENTARY_UNIT).build();
    }
}
