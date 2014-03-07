package eu.ehri.extension.test;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.extension.test.helpers.ServerRunner;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.server.CommunityNeoServer;

import java.io.IOException;

/**
 * Test base class which starts a Neo4j server and loads the
 * resource classes as an unmanaged extension.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class RunningServerTest {

    // Test server port - different from Neo4j default to prevent collisions.
    final static private Integer testServerPort = 7575;

    // Mount point for EHRI resources
    final static private String mountPoint = "ehri";

    private final static ServerRunner runner
            = ServerRunner.getInstance(testServerPort, "eu.ehri.extension", mountPoint);

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        runner.start();
    }

    @AfterClass
    public static void shutdownDatabase() throws Exception {
        runner.stop();
    }

    String getExtensionEntryPointUri() {
        return runner.getServer().baseUri() + mountPoint;
    }

    @Before
    public void setupDb() throws Exception {
        runner.setUpData();
    }

    @After
    public void resetDb() throws Exception {
        runner.tearDownData();
    }
}
