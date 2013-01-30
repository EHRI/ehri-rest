package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import eu.ehri.project.models.events.SystemEvent;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.AbstractFixtureTest;

public class SingleEadImporterTest extends AbstractFixtureTest {

    protected final String SINGLE_EAD = "single-ead.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    // Depends on single-ead.xml
    protected final String IMPORTED_ITEM_ID = "C00001";

    @Test
    public void testImportItemsT() throws Exception {
        Agent agent = manager.getFrame(TEST_REPO, Agent.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount();

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new EadImportManager(graph, agent, validUser)
                .importFile(ios, logMessage);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnit
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 2 more import Event links
        // - 1 more import Event
        assertEquals(count + 6, getNodeCount());

        // Yet we've only created 1 *logical* item...
        assertEquals(1, log.getSuccessful());

        Iterable<Vertex> docs = graph.getVertices("identifier",
                IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(),
                DocumentaryUnit.class);
        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());
    }

    private int getNodeCount() {
        // Note: deprecated use of getAllNodes...
        return toList(graph.getBaseGraph().getRawGraph().getAllNodes()).size();
    }

}
