package eu.ehri.project.importers.test;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;

public class EadListImporterTest extends AbstractFixtureTest {

	protected final String EADLIST = "list-ead.xml";

	// Depends on fixtures
	protected final String TEST_REPO = "r1";

	// Depends on ead-list.xml
	protected final String IMPORTED_ITEM_ID1 = "C00001";
	protected final String IMPORTED_ITEM_ID2 = "C00002";

	@Test
	public void testImportItemsT() throws Exception {
		UserProfile user = graph.frame(graph.getVertex(validUserId),
				UserProfile.class);
		Agent agent = graph.frame(helper.getTestVertex(TEST_REPO), Agent.class);
		final String logMessage = "Importing a single EAD";

		int count = getNodeCount();

		InputStream ios = ClassLoader.getSystemResourceAsStream(EADLIST);
		Action action;
		try {
			action = new EadImportManager(graph, agent, user).importFile(ios,
					logMessage);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			ios.close();
		}

		// How many new nodes will have been created? We should have
		// - 2 more DocumentaryUnit
		// - 2 more DocumentDescription
		// - 1 more import Action
		assertEquals(count + 5, getNodeCount());
		Iterable<Vertex> docs = graph.getVertices("identifier",
				IMPORTED_ITEM_ID1);
		assertTrue(docs.iterator().hasNext());
		DocumentaryUnit unit = graph.frame(docs.iterator().next(),
				DocumentaryUnit.class);
		docs = graph.getVertices("identifier", IMPORTED_ITEM_ID2);
		assertTrue(docs.iterator().hasNext());
		Iterable<Action> actions = unit.getHistory();
		// Check we've only got one action
		assertEquals(1, toList(actions).size());
		assertEquals(2, toList(action.getSubjects()).size());
		assertEquals(logMessage, toList(actions).get(0).getLogMessage());
	}

	private int getNodeCount() {
		// Note: deprecated use of getAllNodes...
		return toList(graph.getBaseGraph().getRawGraph().getAllNodes()).size();
	}

}
