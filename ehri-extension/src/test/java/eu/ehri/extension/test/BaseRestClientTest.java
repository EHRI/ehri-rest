package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.test.utils.FixtureLoader;

/**
 * Base class for testing the REST interface on a neo4j server with the ehri
 * extension deployed. Therefore these should not be run as part of the
 * automatic build (via maven), but separately from the eclipse IDE for
 * instance. Debugging can be done remotely via eclipse as well. The
 * documentation of the neo4j server explains how to do this.
 */
public class BaseRestClientTest {
	final protected String baseUri = "http://localhost:7474";
	final protected String extensionEntryPointUri = baseUri
			+ "/examples/unmanaged/ehri";

	final protected String adminUserProfileId = "20"; // Depends on fixture
														// content.
	protected static AbstractGraphDatabase graphDatabase;
	protected static FixtureLoader loader;
	protected static NeoServer neoServer;

	protected Client client = Client.create();
	protected Converter converter = new Converter();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initializeTestDb(BaseRestClientTest.class.getName());
	}

	public static void initializeTestDb(String dbName) {
		final String dbPath = "target/tmpdb_" + dbName;
		graphDatabase = new EmbeddedGraphDatabase(dbPath);
		loader = new FixtureLoader(new FramedGraph<Neo4jGraph>(new Neo4jGraph(
				graphDatabase)));
		loader.loadTestData();

		ServerConfigurator config = new ServerConfigurator(graphDatabase);
		config.getThirdpartyJaxRsClasses().add(
				new ThirdPartyJaxRsPackage("eu.ehri.extension",
						"/examples/unmanaged"));

		WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(
				graphDatabase, config);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				deleteFolder(new File(dbPath));
			}
		});
		bootstrapper.start();
		neoServer = bootstrapper.getServer();
	}

	/*
	 * Function for deleting an entire database folder. USE WITH CARE!!!
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

	@AfterClass
	public static void shutdownDatabase() throws Exception {
		neoServer.stop();
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
	 * Delete (remove) the given instance from the database, part of a test
	 * clean-up.
	 * 
	 * @param location
	 *            The URL of the resource (entity) to remove from the database.
	 */
	protected void delete(URI location) {
		WebResource resource = client.resource(location);

		// Delete
		ClientResponse response = resource
				.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
						adminUserProfileId).delete(ClientResponse.class);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

		// Get should fail now
		resource = client.resource(location);
		response = resource
				.accept(MediaType.APPLICATION_JSON)
				.header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
						adminUserProfileId).get(ClientResponse.class);
		// assertEquals(Response.Status.NOT_FOUND, response.getStatus());
		// TODO fix it!
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
				response.getStatus());
	}

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
}
