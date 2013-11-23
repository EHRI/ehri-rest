package eu.ehri.extension.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;

/**
 * For testing the REST interface of the neo4j (unmanaged) extension
 */
abstract public class AbstractRestClientTest {

    /**
     * Get the URI of the extension, the base url of the RESTfull interface
     *
     * @return The URI (url) as string
     */
    abstract String getExtensionEntryPointUri();

    /**
     * Get the id of a userProfile with 'admin' privileges.
     *
     * @return The Id as a String
     */
    abstract String getAdminUserProfileId();

    //  ClientConfig clientConfig = new DefaultClientConfig();
    //clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    protected Client client = Client.create();

    protected URI ehriUri(String... segments) {
        UriBuilder builder = UriBuilder.fromPath(getExtensionEntryPointUri());
        for (String segment : segments) {
            builder = builder.segment(segment);
        }
        return builder.build();
    }

    protected WebResource.Builder jsonCallAs(String user, URI uri) {
        return client.resource(uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
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

    /*** Helpers ***/

    /**
     * NOTE not sure how this handles UTF8
     *
     * @throws java.io.IOException
     */
    protected String readFileAsString(String filePath)
            throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(this
                .getClass().getClassLoader().getResourceAsStream(filePath)));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();

        return fileData.toString();
    }

}
