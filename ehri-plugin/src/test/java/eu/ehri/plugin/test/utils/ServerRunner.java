package eu.ehri.plugin.test.utils;

import java.io.File;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.ServerConfigurator;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.test.utils.FixtureLoader;
import eu.ehri.project.test.utils.GraphCleaner;

/**
 * Class that handles running a test Neo4j server.
 * 
 */
public class ServerRunner {

    protected AbstractGraphDatabase graphDatabase;
    protected WrappingNeoServerBootstrapper bootstrapper;
    protected FixtureLoader loader;
    protected GraphCleaner cleaner;
    protected NeoServer neoServer;
    protected ServerConfigurator config;
    private FramedGraph<Neo4jGraph> framedGraph;

    /**
     * Initialise a new Neo4j Server with the given db name and port.
     * 
     * @param dbName
     * @param dbPort
     */
    public ServerRunner(String dbName, Integer dbPort) {
        // TODO: Work out a better way to configure the path
        final String dbPath = "target/tmpdb_" + dbName;
        graphDatabase = new EmbeddedGraphDatabase(dbPath);
        framedGraph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(graphDatabase));

        // Initialize the fixture loader and cleaner
        loader = new FixtureLoader(framedGraph);
        cleaner = new GraphCleaner(framedGraph);

        // Server configuration. TODO: Work out how to disable server startup
        // and load logging so the test output isn't so noisy...
        config = new ServerConfigurator(graphDatabase);
        config.configuration().setProperty("org.neo4j.server.webserver.port",
                dbPort.toString());

        bootstrapper = new WrappingNeoServerBootstrapper(graphDatabase, config);

        // Attempt to ensure database is erased from the disk when
        // the runtime shuts down. This improves repeatability, because
        // if it is still there it'll be appended to on the next run.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                deleteFolder(new File(dbPath));
            }
        });
    }

    /**
     * Get the configurator for the test db. This allows adjusting config before
     * starting it up.
     * 
     * @return
     */
    public ServerConfigurator getConfigurator() {
        return config;
    }

    /**
     * Initialise a new graph database in a given location. This should be
     * unique for each superclass, because otherwise problems can be encountered
     * when another test suite starts up whilst a database is in the process of
     * shutting down.
     * 
     */
    public void start() {
        bootstrapper.start();
    }

    public void setUp() throws Exception {
        loader.loadTestData();
    }

    public void tearDown() {
        cleaner.clean();
    }

    /**
     * Stop the server
     */
    public void stop() {
        bootstrapper.stop();
    }

    /**
     * Function for deleting an entire database folder. USE WITH CARE!!!
     * 
     * @param folder
     */
    protected void deleteFolder(File folder) {
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
