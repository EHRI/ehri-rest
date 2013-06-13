package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistance.Bundle;

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
            throws
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

    //@Test
    public void testPermissionSetPermissionDenied()
            throws UniformInterfaceException, IOException {

        // Test a user setting his own permissions over REST - this should
        // obviously fail...
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME)
                .entity(new ObjectMapper().writeValueAsBytes(getTestMatrix()))
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());
        // TODO: Figure out why no content ever seems to be returned here?
    }

    @Test
    public void testGivingBadPermsErrorsCorrectly()
            throws
            UniformInterfaceException, IOException {

        // If we give a permission matrix for a content type that doesn't
        // exist we should get a DeserializationError in return.
        Map<String, List<String>> testMatrix = getTestMatrix();
        testMatrix.put(
                "IDONTEXIST",
                new ImmutableList.Builder<String>().add(
                        PermissionType.CREATE.getName()).build());

        // Set the permission via REST
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + Entities.PERMISSION + "/" + LIMITED_USER_NAME);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(new ObjectMapper().writeValueAsBytes(testMatrix))
                .post(ClientResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
    }

    @Test
    public void testSettingGlobalPermissions() throws
            UniformInterfaceException, IOException {

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

    @Test
    public void testSettingScopedPermissions() throws Exception {
        // Grant permissions for a user to create items within this scope.

        String r2 = "r2";
        String r3 = "r3";
        
        // The user shouldn't be able to create docs with r2
        WebResource resource = client.resource(getCreationUriFor(r2));
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Or r3...
        resource = client.resource(getCreationUriFor(r3));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Now grant the user permissions to create just within
        // the scope of r2
        String permData = "{\"documentaryUnit\": [\"create\"]}";

        URI grantUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.PERMISSION).segment(LIMITED_USER_NAME)
                .segment("scope").segment(r2)
                .build();

        resource = client.resource(grantUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(permData)
                .post(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Now creation should succeed...
        resource = client.resource(getCreationUriFor(r2));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);
        assertEquals(Response.Status.CREATED.getStatusCode(),
                response.getStatus());

        // But r3 should still fail...
        // Or r3...
        resource = client.resource(getCreationUriFor(r3));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // And the user himself should not be able to grant
        // others the ability to create within that scope.
        String otherUserName = "linda";
        String grantPermData = "{\"documentaryUnit\": [\"grant\"]}";
        URI otherGrantUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.PERMISSION).segment(otherUserName)
                .segment("scope").segment(r2)
                .build();

        resource = client.resource(otherGrantUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(grantPermData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

    }

    @Test
    public void testSettingItemPermissions() throws
            UniformInterfaceException, IOException,
            DeserializationError {

        // Fetch an existing item's data
        String targetResourceId = "c4";
        URI targetResourceUri = UriBuilder
                .fromPath(getExtensionEntryPointUri())
                .segment(Entities.DOCUMENTARY_UNIT).segment(targetResourceId)
                .build();

        WebResource resource = client.resource(targetResourceUri);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // First try and update the item
        String testUpdateString = Bundle
                .fromString(response.getEntity(String.class))
                .withDataValue("testKey", "testValue").toJson();

        resource = client.resource(targetResourceUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(testUpdateString)
                .put(ClientResponse.class);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                response.getStatus());

        // Now grant the user permissions to update and delete just on this item
        String permData = "[\"update\", \"delete\"]";

        // Set the permission via REST
        resource = client.resource(getExtensionEntryPointUri() + "/"
                + Entities.PERMISSION + "/" + LIMITED_USER_NAME + "/" + targetResourceId);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(permData)
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Retry the create action
        resource = client.resource(targetResourceUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).entity(testUpdateString)
                .put(ClientResponse.class);
        // Should get UPDATED this time...
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Finally, delete the item
        resource = client.resource(targetResourceUri);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        LIMITED_USER_NAME).delete(ClientResponse.class);

        // Should get CREATED this time...
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private List<Map<String, Map<String, List<String>>>> getInheritedMatrix(
            String json) throws
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
            put(ContentTypes.REPOSITORY.getName(), new LinkedList<String>() {{
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
                .segment(Entities.REPOSITORY).segment(TEST_HOLDER_IDENTIFIER)
                .segment(Entities.DOCUMENTARY_UNIT).build();
    }

    private URI getCreationUriFor(String id) {
        URI creationUri = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.REPOSITORY).segment(id)
                .segment(Entities.DOCUMENTARY_UNIT).build();
        return creationUri;
    }
}
