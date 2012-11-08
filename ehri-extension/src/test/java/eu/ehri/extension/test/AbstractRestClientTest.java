package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.persistance.Converter;

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

    protected Client client = Client.create();
    protected Converter converter = new Converter();

    /**
     * Tests if we have an admin user, we need that user for doing all the other
     * tests
     */
    @Test
    public void testAdminGetUserProfile() throws Exception {
        // get the admin user profile
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + EntityTypes.USER_PROFILE + "/"
                + getAdminUserProfileId());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    /*** Helpers ***/

    /**
     * NOTE not sure how this handles UTF8
     * 
     * @param filePath
     * @return
     * @throws java.io.IOException
     */
    protected String readFileAsString(String filePath)
            throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(this
                .getClass().getClassLoader().getResourceAsStream(filePath)));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();

        return fileData.toString();
    }

}
