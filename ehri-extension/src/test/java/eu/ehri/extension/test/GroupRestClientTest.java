package eu.ehri.extension.test;

import com.google.common.collect.Sets;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.extension.GroupResource;
import eu.ehri.project.definitions.Entities;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Set;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroupRestClientTest extends BaseRestClientTest {

    static final String TEST_GROUP_NAME = "admin";
    static final String CURRENT_ADMIN_USER = "mike";
    static final String NON_ADMIN_USER = "reto";

    @Test
    public void testCreateGroup() throws Exception {
        // Create
        String jsonGroupTestString = "{\"type\": \"group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.GROUP)).entity(jsonGroupTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);
        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testCreateGroupWithMembers() throws Exception {
        // Create
        String jsonGroupTestString = "{\"type\": \"group\", \"data\":{\"identifier\": \"jmp\", \"name\": \"JMP\"}}";
        URI uri = UriBuilder.fromPath(getExtensionEntryPointUri()).segment(Entities.GROUP)
                .queryParam(GroupResource.MEMBER_PARAM, "linda").build();
        ClientResponse response = client.resource(uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .entity(jsonGroupTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);
        // Get created doc via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(),
                UriBuilder.fromUri(location).segment("list").build())
                .get(ClientResponse.class);

        assertStatus(OK, response);

        // check results
        Set<String> ids = getIdsFromEntityListJson(response.getEntity(String.class));
        assertTrue(ids.contains("linda"));
        assertEquals(1, ids.size());
    }

    @Test
    public void testAddUser() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.GROUP, TEST_GROUP_NAME, NON_ADMIN_USER))
                .post(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testRemoveUser() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.GROUP, TEST_GROUP_NAME, CURRENT_ADMIN_USER))
                .delete(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testListGroupMembers() throws Exception {
        final String GROUP_ID = "kcl";

        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.GROUP, GROUP_ID, "list"))
                .get(ClientResponse.class);

        assertStatus(OK, response);

        // check results
        //System.out.println(response.getEntity(String.class));
        Set<String> ids = getIdsFromEntityListJson(response.getEntity(String.class));
        // for 'kcl' it should be 'mike', 'reto' and nothing else
        assertTrue(ids.contains("mike"));
        assertTrue(ids.contains("reto"));
        assertEquals(2, ids.size());
    }

    /**
     * Helpers **
     */

    private Set<String> getIdsFromEntityListJson(String jsonString) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonString);
        Set<String> ids = Sets.newHashSet();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            ids.add(item.getString("id"));
        }
        return ids;
    }
}
