package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.plugin.test.utils.ServerRunner;
import eu.ehri.project.persistance.Converter;

/**
 * Base class for testing the REST interface on a neo4j server with the ehri
 * extension deployed. Therefore these should not be run as part of the
 * automatic build (via maven), but separately from the eclipse IDE for
 * instance. Debugging can be done remotely via eclipse as well. The
 * documentation of the neo4j server explains how to do this.
 */
public class BaseRestClientTest {
    // Test server port - different from Neo4j default to prevent collisions.
    final static protected Integer testServerPort = 7575;
    // Test server host
    final static protected String baseUri = "http://localhost:"
            + testServerPort;
    // Mount point for EHRI resources
    final static protected String mountPoint = "/";
    final static protected String extensionEntryPointUri = baseUri + mountPoint
            + "ehri";

    // Admin user prefix - depends on fixture data
    final static protected String adminUserProfileId = "20";

    protected static ServerRunner runner;

    protected Client client = Client.create();
    protected Converter converter = new Converter();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(BaseRestClientTest.class.getName());
    }

    /**
     * Initialise a new graph database in a given location. This should be
     * unique for each superclass, because otherwise problems can be encountered
     * when another test suite starts up whilst a database is in the process of
     * shutting down.
     * 
     * @param dbName
     */
    protected static void initializeTestDb(String dbName) {
        runner = new ServerRunner(dbName, testServerPort);
        runner.getConfigurator()
                .getThirdpartyJaxRsClasses()
                .add(new ThirdPartyJaxRsPackage(EhriNeo4jFramedResource.class
                        .getPackage().getName(), mountPoint));
        runner.start();
    }

    /**
     * Shut down database when test suite has run.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void shutdownDatabase() throws Exception {
        runner.stop();
    }

    /**
     * Tests if we have an admin user, we need that user for doing all the other
     * tests
     * 
     * curl -v -X GET -H "Authorization: 80497" -H "Accept: application/json"
     * http://localhost:7474/examples/unmanaged/ehri/userProfile/80497
     * 
     */
    @Test
    public void testAdminGetUserProfile() throws Exception {
        // get the admin user profile
        WebResource resource = client.resource(extensionEntryPointUri
                + "/userProfile" + "/" + adminUserProfileId);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // TODO check it has a group with 'admin' rights
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

    /**
     * Function for deleting an entire database folder. USE WITH CARE!!!
     * 
     * @param folder
     */
    protected static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
