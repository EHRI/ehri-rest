package eu.ehri.extension.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import eu.ehri.project.models.base.AccessibleEntity;

public class PermissionRestClientTest extends BaseRestClientTest {

    static final String LIMITED_USER_NAME = "reto";
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
                + "/" + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        
        Map<String, Map<String, Boolean>> currentMatrix = getMatrix(response.getEntity(String.class));
        
        // Check we don't ALREADY have documentaryUnit -> create/delete perms
        assertFalse(currentMatrix.get(EntityTypes.DOCUMENTARY_UNIT).get(PermissionTypes.CREATE));
        assertFalse(currentMatrix.get(EntityTypes.DOCUMENTARY_UNIT).get(PermissionTypes.DELETE));

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
        resource = client.resource(getExtensionEntryPointUri()
                + "/" + EntityTypes.PERMISSION + "/" + EntityTypes.USER_PROFILE + "/" + LIMITED_USER_NAME);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(),
                response.getStatus());
        
        Map<String, Map<String, Boolean>> newMatrix = getMatrix(response.getEntity(String.class));
        
        // Check we don't ALREADY have documentaryUnit -> create/delete perms
        assertTrue(newMatrix.get(EntityTypes.DOCUMENTARY_UNIT).get(PermissionTypes.CREATE));
        assertTrue(newMatrix.get(EntityTypes.DOCUMENTARY_UNIT).get(PermissionTypes.DELETE));
    }


    @Test
    public void testSettingGlobalPermissions() throws JsonGenerationException,
            JsonMappingException, UniformInterfaceException, IOException {
        
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
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
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        // Should get CREATED this time...
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // Get the item id
        String id = new ObjectMapper()
                .readTree(response.getEntity(String.class)).path("data")
                .path(AccessibleEntity.IDENTIFIER_KEY).asText();

        // Finally, delete the item
        resource = client.resource(getExtensionEntryPointUri()
                + "/documentaryUnit/" + id);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).delete(ClientResponse.class);

        // Should get CREATED this time...
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private Map<String, Map<String, Boolean>> getMatrix(String json)
            throws JsonParseException, JsonMappingException, IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        TypeReference<HashMap<String, Map<String, Boolean>>> typeRef = new TypeReference<HashMap<String, Map<String, Boolean>>>() {
        };
        return mapper.readValue(json, typeRef);
    }
    
    @SuppressWarnings("serial")
    private Map<String, Map<String, Boolean>> getTestMatrix() {
        // @formatter:off
        Map<String,Map<String,Boolean>> matrix = new HashMap<String, Map<String,Boolean>>() {{
            put(EntityTypes.DOCUMENTARY_UNIT, new HashMap<String,Boolean>() {{
                put(PermissionTypes.CREATE, true);
                put(PermissionTypes.DELETE, true);
            }});
        }};
        // @formatter:on
        return matrix;
    }    
}
