package eu.ehri.extension.test;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.ServerBuilder;

import java.io.IOException;

/**
 * Test base class which starts a Neo4j server and loads the
 * resource classes as an unmanaged extension.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class RunningServerTest {

    // Graph factory.
    final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    // Test server port - different from Neo4j default to prevent collisions.
    final static private Integer testServerPort = 7575;

    // Mount point for EHRI resources
    final static private String mountPoint = "ehri";

    private static CommunityNeoServer neoServer;
    private static FixtureLoader fixtureLoader;
    private static GraphCleaner<? extends TransactionalGraph> graphCleaner;

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        neoServer = ServerBuilder.server()
                .onPort(testServerPort)
                .withThirdPartyJaxRsPackage("eu.ehri.extension", "/" + mountPoint)
                .build();
        neoServer.start();
        FramedGraph<? extends TransactionalGraph> framedGraph
                = graphFactory.create(new Neo4jGraph(neoServer.getDatabase().getGraph()));
        fixtureLoader = FixtureLoaderFactory.getInstance(framedGraph);
        graphCleaner = new GraphCleaner(framedGraph);
    }

    @AfterClass
    public static void shutdownDatabase() throws Exception {
        neoServer.stop();
    }

    String getExtensionEntryPointUri() {
        return neoServer.baseUri() + mountPoint;
    }

    @Before
    public void setupDb() throws Exception {
        fixtureLoader.loadTestData();
    }

    @After
    public void resetDb() throws Exception {
        graphCleaner.clean();
    }
}
