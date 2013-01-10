package eu.ehri.extension.test;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import eu.ehri.extension.AbstractAccessibleEntityResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.extension.test.utils.ServerRunner;

/**
 * Base class for testing the REST interface on a 'embedded' neo4j server.
 */
public class BaseRestClientTest extends AbstractRestClientTest {
    // Test server port - different from Neo4j default to prevent collisions.
    final static private Integer testServerPort = 7575;
    // Test server host
    final static private String baseUri = "http://localhost:" + testServerPort;
    // Mount point for EHRI resources
    final static private String mountPoint = "/ehri";
    final static private String extensionEntryPointUri = baseUri + mountPoint;

    // Admin user prefix - depends on fixture data
    final static private String adminUserProfileId = "mike";

    protected static ServerRunner runner;

    protected String getBaseUri() {
        return baseUri;
    }
    
    @Override
    String getExtensionEntryPointUri() {
        return extensionEntryPointUri;
    }

    @Override
    String getAdminUserProfileId() {
        return adminUserProfileId;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(BaseRestClientTest.class.getName());
    }

    @Before
    public void setupDb() throws Exception {
        runner.setUp();
    }

    @After
    public void resetDb() throws Exception {
        runner.tearDown();
    }

    /**
     * Initialise a new graph database in a given location. This should be
     * unique for each class, because otherwise problems can be encountered when
     * another test suite starts up whilst a database is in the process of
     * shutting down.
     * 
     * @param dbName
     */
    public static void initializeTestDb(String dbName) {
        runner = new ServerRunner(dbName, testServerPort);
        runner.getConfigurator()
                .getThirdpartyJaxRsClasses()
                .add(new ThirdPartyJaxRsPackage(AbstractAccessibleEntityResource.class
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

    /*** Helpers ***/

    /**
     * Function for fetching a list of entities with the given EntityType
     */
    protected List<Map<String, Object>> getEntityList(String entityType,
            String userId) throws Exception {
        WebResource resource = client.resource(getExtensionEntryPointUri()
                + "/" + entityType + "/list");
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
