package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import eu.ehri.project.models.events.SystemEvent;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.test.AbstractFixtureTest;

public class HierarchicalEadImporterTest extends AbstractImporterTest {

	protected final String HIERARCHICAL_EAD = "hierarchical-ead.xml";
	 private static final Logger logger = LoggerFactory
	            .getLogger(HierarchicalEadImporterTest.class);

	// Depends on fixtures
	protected final String TEST_REPO = "r1";

	// Depends on hierarchical-ead.xml
	protected final String IMPORTED_ITEM_ID = "C00001";
	protected final String IMPORTED_ITEM_ID_N1 = "C00001-1";
	protected final String IMPORTED_ITEM_ID_N1_N1 = "C00001-1-1";
	protected final String IMPORTED_ITEM_ID_N1_N2 = "C00001-1-1";

	@Test
	public void testImportItemsT() throws Exception {
		logger.info("logger.info");
		System.out.println("sysout");
		logger.error("logger.error");
		Agent agent = manager.getFrame(TEST_REPO, Agent.class);
		final String logMessage = "Importing a single EAD";

		int count = getNodeCount(graph);

		InputStream ios = ClassLoader
				.getSystemResourceAsStream(HIERARCHICAL_EAD);
		ImportLog log = new EadImportManager(graph, agent, validUser).importFile(ios, logMessage);

		// How many new nodes will have been created? We should have
		// - 4 more DocumentaryUnits
		// - 4 more DocumentDescription
		// - 1 more DatePeriod
		// - 5 more import Event links
        // - 1 more import Event
		assertEquals(count + 15, getNodeCount(graph));
		Iterable<Vertex> docs = graph.getVertices("identifier",
				IMPORTED_ITEM_ID);
		assertTrue(docs.iterator().hasNext());
		DocumentaryUnit unit = graph.frame(docs.iterator().next(),
				DocumentaryUnit.class);

		// check the child items
		DocumentaryUnit c1 = graph.frame(
				getVertexByIdentifier(IMPORTED_ITEM_ID_N1),
				DocumentaryUnit.class);
		DocumentaryUnit cc1 = graph.frame(
				getVertexByIdentifier(IMPORTED_ITEM_ID_N1_N1),
				DocumentaryUnit.class);
		DocumentaryUnit cc2 = graph.frame(
				getVertexByIdentifier(IMPORTED_ITEM_ID_N1_N2),
				DocumentaryUnit.class);

		// Ensure that the first child's parent is unit
		assertEquals(unit, c1.getParent());

		// Ensure the grandkids parents is c1
		assertEquals(c1, cc1.getParent());
		assertEquals(c1, cc2.getParent());

		// Ensure unit the the grandparent of cc1
		List<DocumentaryUnit> ancestors = toList(cc1.getAncestors());
		assertEquals(unit, ancestors.get(ancestors.size() - 1));

		// Ensure the import action has the right number of subjects.
		Iterable<SystemEvent> actions = unit.getHistory();
		// Check we've only got one action
		assertEquals(1, toList(actions).size());
		assertEquals(logMessage, log.getAction().getLogMessage());

		List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
		assertEquals(4, subjects.size());
		assertEquals(log.getSuccessful(), subjects.size());

	}

	// Helpers...

	private Vertex getVertexByIdentifier(String id) {
		Iterable<Vertex> docs = graph.getVertices("identifier", id);
		return docs.iterator().next();
	}
}
