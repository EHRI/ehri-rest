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

public class EadListImporterTest extends AbstractImporterTest {

    protected final String EADLIST = "list-ead.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    // Depends on ead-list.xml
    protected final String IMPORTED_ITEM_ID1 = "C00001";
    protected final String IMPORTED_ITEM_ID2 = "C00002";

    @Test
    public void testImportItemsT() throws Exception {
        Agent agent = manager.getFrame(TEST_REPO, Agent.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(EADLIST);
        ImportLog log;
        try {
            log = new EadImportManager(graph, agent, validUser).importFile(ios,
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
        // - 2 more import Event links
        // - 1 more import Event
        assertEquals(count + 8, getNodeCount(graph));
        Iterable<Vertex> docs = graph.getVertices("identifier",
                IMPORTED_ITEM_ID1);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(),
                DocumentaryUnit.class);
        docs = graph.getVertices("identifier", IMPORTED_ITEM_ID2);
        assertTrue(docs.iterator().hasNext());
        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(2, toList(log.getAction().getSubjects()).size());
        assertEquals(logMessage, actions.get(0).getLogMessage());
    }
}
