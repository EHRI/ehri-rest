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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to obtain a server.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ServerRunner {

    private static ServerRunner INSTANCE = null;

    // Graph factory.
    final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    private final int port;
    private final String jaxRxPackage;
    private final String mountPoint;

    private Level logLevel = Level.OFF;

    private static FixtureLoader fixtureLoader = null;
    private static GraphCleaner<? extends TransactionalGraph> graphCleaner = null;

    private final static Logger sunLogger = Logger.getLogger("com.sun.jersey");
    private final static Logger neoLogger = Logger.getLogger("org.neo4j.server");

    private CommunityNeoServer neoServer;

    private ServerRunner(int port, String jaxRxPackage, String mountPoint) {
        this.port = port;
        this.jaxRxPackage = jaxRxPackage;
        this.mountPoint = mountPoint;
    }

    public static ServerRunner getInstance(int port, String jaxRxPackage, String mountPoint) {
        if (INSTANCE == null) {
            INSTANCE = new ServerRunner(port, jaxRxPackage, mountPoint);
        }
        return INSTANCE;
    }

    public void start() throws IOException {
//        sunLogger.setLevel(logLevel);
//        neoLogger.setLevel(logLevel);
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

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public void setUpData() {
        if (fixtureLoader != null) {
            fixtureLoader.loadTestData();
        }
    }

    public void tearDownData() {
        if (graphCleaner != null) {
            graphCleaner.clean();
        }
    }

    public void stop() {
        if (neoServer != null) {
            neoServer.stop();
        }
    }
}
