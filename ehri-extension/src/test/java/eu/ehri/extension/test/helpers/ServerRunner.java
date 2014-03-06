package eu.ehri.extension.test.helpers;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.ServerBuilder;

import java.io.IOException;

/**
 * Utility class to obtain a server.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ServerRunner {

    // Graph factory.
    final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    private final int port;
    private final String jaxRxPackage;
    private final String mountPoint;

    private static FixtureLoader fixtureLoader;
    private static GraphCleaner<? extends TransactionalGraph> graphCleaner;


    private CommunityNeoServer neoServer;

    public ServerRunner(int port, String jaxRxPackage, String mountPoint) {
        this.port = port;
        this.jaxRxPackage = jaxRxPackage;
        this.mountPoint = mountPoint;
    }

    public void start() throws IOException {
        neoServer = ServerBuilder.server()
                .onPort(port)
                .withThirdPartyJaxRsPackage(jaxRxPackage, "/" + mountPoint)
                .build();
        neoServer.start();
        FramedGraph<? extends TransactionalGraph> framedGraph
                = graphFactory.create(new Neo4jGraph(neoServer.getDatabase().getGraph()));
        fixtureLoader = FixtureLoaderFactory.getInstance(framedGraph);
        graphCleaner = new GraphCleaner(framedGraph);
    }

    public CommunityNeoServer getServer() {
        return neoServer;
    }

    public void setUpData() {
        fixtureLoader.loadTestData();
    }

    public void tearDownData() {
        graphCleaner.clean();
    }

    public void stop() {
        if (neoServer != null) {
            neoServer.stop();
        }
    }
}
