package eu.ehri.extension.test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;

/**
 * Base class for testing the REST interface on a 'embedded' neo4j server.
 */
public class BaseRestClientTest extends RunningServerTest {

    protected static Client client = Client.create();

    protected static final ObjectMapper jsonMapper = new ObjectMapper();

    protected static Pattern paginationPattern = Pattern.compile("offset=(-?\\d+); limit=(-?\\d+); total=(-?\\d+)");

    // Admin user prefix - depends on fixture data
    final static private String adminUserProfileId = "mike";

    // Regular user
    final static private String regularUserProfileId = "reto";

    protected String getAdminUserProfileId() {
        return adminUserProfileId;
    }

    protected String getRegularUserProfileId() {
        return regularUserProfileId;
    }

    /**
     * Tests if we have an admin user, we need that user for doing all the other
     * tests
     */
    @Test
    public void testAdminGetUserProfile() throws Exception {
        // get the admin user profile
        WebResource resource = client.resource(
                ehriUri(Entities.USER_PROFILE, getAdminUserProfileId()));
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    /**
     * Helpers **
     */

    protected List<Map<String, Object>> getItemList(String entityType, String userId) throws Exception {
        return getItemList(entityType, userId, new MultivaluedMapImpl());
    }

    /**
     * Get a list of items at some url, as the given user.
     */
    protected List<Map<String, Object>> getItemList(String url, String userId,
            MultivaluedMap<String, String> params) throws Exception {
        WebResource resource = client.resource(getExtensionEntryPointUri() + url).queryParams(params);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, userId)
                .get(ClientResponse.class);
        String json = response.getEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<LinkedList<HashMap<String, Object>>> typeRef = new TypeReference<LinkedList<HashMap<String, Object>>>() {
        };
        return mapper.readValue(json, typeRef);
    }

    /**
     * Function for fetching a list of entities with the given EntityType
     */
    protected List<Map<String, Object>> getEntityList(String entityType,
            String userId) throws Exception {
        return getEntityList(entityType, userId, new MultivaluedMapImpl());
    }

    /**
     * Function for fetching a list of entities with the given EntityType,
     * and some additional parameters.
     */
    protected List<Map<String, Object>> getEntityList(String entityType,
            String userId, MultivaluedMap<String, String> params) throws Exception {
        return getItemList("/" + entityType + "/list", userId, params);
    }

    protected Integer getPaginationPage(ClientResponse response) {
        MultivaluedMap<String,String> headers = response.getHeaders();
        String range = headers.getFirst("Content-Range");
        if (range != null && range.matches(paginationPattern.pattern())) {
            Matcher matcher = paginationPattern.matcher(range);
            matcher.find();
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    protected Integer getPaginationTotal(ClientResponse response) {
        MultivaluedMap<String,String> headers = response.getHeaders();
        String range = headers.getFirst("Content-Range");
        if (range != null && range.matches(paginationPattern.pattern())) {
            Matcher matcher = paginationPattern.matcher(range);
            matcher.find();
            return Integer.valueOf(matcher.group(3));
        }
        return null;
    }

    protected Long getEntityCount(String entityType,
            String userId) throws Exception {
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + entityType + "/count");
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, userId)
                .get(ClientResponse.class);
        String json = response.getEntity(String.class);
        TypeReference<Long> typeRef = new TypeReference<Long>() {
        };
        return jsonMapper.readValue(json, typeRef);
    }

    protected UriBuilder ehriUriBuilder(String... segments) {
        UriBuilder builder = UriBuilder.fromPath(getExtensionEntryPointUri());
        for (String segment : segments) {
            builder = builder.segment(segment);
        }
        return builder;
    }

    protected URI ehriUri(String... segments) {
        return ehriUriBuilder(segments).build();
    }

    protected WebResource.Builder jsonCallAs(String user, URI uri) {
        return callAs(user, uri)
                .type(MediaType.APPLICATION_JSON);
    }

    protected WebResource.Builder callAs(String user, URI uri) {
        return client.resource(uri)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user);
    }

    public void assertStatus(ClientResponse.Status status, ClientResponse response) {
        org.junit.Assert.assertEquals(status.getStatusCode(), response.getStatus());
    }

    protected WebResource.Builder jsonCallAs(String user, String... segments) {
        UriBuilder builder = UriBuilder.fromPath(getExtensionEntryPointUri());
        for (String segment : segments) {
            builder = builder.segment(segment);
        }
        URI uri = builder.build();
        return client.resource(uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user);
    }

    protected String readFileAsString(String filePath)
            throws java.io.IOException {
        URL url = Resources.getResource(filePath);
        return Resources.toString(url, Charsets.UTF_8);
    }
}
