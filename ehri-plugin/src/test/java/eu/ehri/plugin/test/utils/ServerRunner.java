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

public class ServerRunner {

	private Integer dbPort = 7575;
	private String dbName = "tmpdb";
	
	protected AbstractGraphDatabase graphDatabase;
	protected FixtureLoader loader;
	protected NeoServer neoServer;
	protected ServerConfigurator config;

	public ServerRunner(String dbName, Integer dbPort, ServerConfigurator config) {
		this.dbPort = dbPort;
		this.dbName = dbName;
		this.config = config;
	}
	
	/**
	 * Initialise a new graph database in a given location. This should be
	 * unique for each superclass, because otherwise problems can be encountered
	 * when another test suite starts up whilst a database is in the process of
	 * shutting down.
	 * 
	 */
	public NeoServer initialize() {
		final String dbPath = "target/tmpdb_" + dbName;
		graphDatabase = new EmbeddedGraphDatabase(dbPath);
		loader = new FixtureLoader(new FramedGraph<Neo4jGraph>(new Neo4jGraph(
				graphDatabase)));
		loader.loadTestData();

		// Server configuration. TODO: Work out how to disable server startup
		// and load logging so the test output isn't so noisy...
		if (config == null)
			config = new ServerConfigurator(graphDatabase);
		config.configuration().setProperty("org.neo4j.server.webserver.port",
				dbPort.toString());

		WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(
				graphDatabase, config);
		// Attempt to ensure database is erased from the disk when
		// the runtime shuts down. This improves repeatability, because
		// if it is still there it'll be appended to on the next run.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				deleteFolder(new File(dbPath));
			}
		});
		bootstrapper.start();
		return bootstrapper.getServer(); 
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
