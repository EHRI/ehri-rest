package eu.ehri.extension.test.utils;

import java.io.File;

import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;

/**
 * Class that handles running a test Neo4j server.
 */
public class ServerRunner extends WrappingNeoServerBootstrapper {

    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    protected final FixtureLoader loader;
    protected final GraphCleaner cleaner;
    private FramedGraph<Neo4jGraph> framedGraph;
    private boolean isRunning = false;

    public ServerRunner(GraphDatabaseAPI graphDatabase, ServerConfigurator config) {
        super(graphDatabase, config);
        framedGraph = graphFactory.create((new Neo4jGraph(graphDatabase)));
        loader = FixtureLoaderFactory.getInstance(framedGraph);
        cleaner = new GraphCleaner(framedGraph);
    }

    /**
     * Initialise a new Neo4j Server with the given db name and port.
     *
     * @param dbName
     * @param dbPort
     */
    public static ServerRunner getInstance(String dbName, Integer dbPort) {
        // TODO: Work out a better way to configure the path
        final String dbPath = "target/tmpdb_" + dbName;
        GraphDatabaseAPI graphDatabase = (GraphDatabaseAPI) new GraphDatabaseFactory()
                .newEmbeddedDatabase(dbPath);

        // Initialize the fixture loader and cleaner
        // Server configuration. TODO: Work out how to disable server startup
        // and load logging so the test output isn't so noisy...
        ServerConfigurator config = new ServerConfigurator(graphDatabase);
        config.configuration().setProperty("org.neo4j.server.webserver.port",
                dbPort.toString());
        config.configuration().setProperty("org.neo4j.server.webserver.port",
                dbPort.toString());
        config.configuration().setProperty("org.neo4j.dbpath", dbPath);

        // FIXME: Work out how to turn off server logging. The config below
        // doesn't
        // work but I'm leaving it in place so I know what's been tried!
        config.configuration().setProperty(
                "java.util.logging.ConsoleHandler.level", "OFF");
        config.configuration().setProperty("org.neo4j.server.logging.level",
                "ERROR");

        // Attempt to ensure database is erased from the disk when
        // the runtime shuts down. This improves repeatability, because
        // if it is still there it'll be appended to on the next run.
        /*Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                deleteFolder(new File(dbPath));
            }
        });*/

        return new ServerRunner(graphDatabase, config);
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Integer start() {
        isRunning = true;
        return super.start();
    }

    @Override
    public int stop(int stopArg) {
        isRunning = false;
        return super.stop(stopArg);
    }

    public void setUp() {
        loader.loadTestData();
    }

    public void tearDown() {
        cleaner.clean();
    }

    /**
     * Get the configurator for the test db. This allows adjusting config before
     * starting it up.
     *
     * @return
     */
    public Configurator getConfigurator() {
        return createConfigurator();
    }

    @Override
    protected void addShutdownHook() {
        Runtime.getRuntime()
                .addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        if (server != null && isRunning) {
                            server.stop();
                        }
                        deleteFolder(
                                new File(
                                        createConfigurator()
                                                .configuration()
                                                .getString("org.neo4j.dbpath")));
                    }
                });
    }

    /**
     * Function for deleting an entire database folder. USE WITH CARE!!!
     *
     * @param folder
     */
    private static void deleteFolder(File folder) {
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
