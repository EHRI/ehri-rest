package eu.ehri.extension.test;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;

import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.plugin.test.utils.ServerRunner;

/**
 * Base class for testing the REST interface on a 'embedded' neo4j server. 
 */
public class BaseRestClientTest extends AbstractRestClientTest {
    // Test server port - different from Neo4j default to prevent collisions.
    final static private Integer testServerPort = 7575;
    // Test server host
    final static private String baseUri = "http://localhost:"
            + testServerPort;
    // Mount point for EHRI resources
    final static private String mountPoint = "/";
    final static private String extensionEntryPointUri = baseUri + mountPoint
            + "ehri";

    // Admin user prefix - depends on fixture data
    final static private String adminUserProfileId = "20";

    protected static ServerRunner runner;

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

    /**
     * Initialise a new graph database in a given location. This should be
     * unique for each class, because otherwise problems can be encountered
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

    /*** Helpers ***/


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
