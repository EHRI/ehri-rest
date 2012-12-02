package eu.ehri.project.importers.test;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.importers.EadImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.AbstractFixtureTest;

public class EadUpdateImporterTest extends AbstractFixtureTest {

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
        ImportLog log = new EadImportManager(graph, agent, validUser).importFile(ios, logMessage);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnit
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more import Action        
        assertEquals(count + 4, getNodeCount());
        
        // Yet we've only created 1 *logical* item...
        assertEquals(1, log.getSuccessful());
        
        Iterable<Vertex> docs = graph.getVertices("identifier",
                IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(),
                DocumentaryUnit.class);
        Iterable<Action> actions = unit.getHistory();
        // Check we've only got one action
        assertEquals(1, toList(actions).size());
        assertEquals(logMessage, toList(actions).get(0).getLogMessage());
        
        // Now re-import the same file
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log2 = new EadImportManager(graph, agent, validUser).importFile(ios2, logMessage);
        
        // We should only have one more node, for the action
        assertEquals(count + 5, getNodeCount());
        // And one logical item should've been updated
        assertEquals(1, log2.getUpdated());
        
    }

    private int getNodeCount() {
        // Note: deprecated use of getAllNodes...
        return toList(graph.getBaseGraph().getRawGraph().getAllNodes()).size();
    }

}
