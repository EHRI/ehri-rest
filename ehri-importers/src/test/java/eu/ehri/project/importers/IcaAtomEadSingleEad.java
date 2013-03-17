/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.importers.test.AbstractImporterTest;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class IcaAtomEadSingleEad extends AbstractImporterTest{
    protected final String SINGLE_EAD = "single-ead.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    // Depends on single-ead.xml
    protected final String IMPORTED_ITEM_ID = "C00001";

    @Test
    public void testImportItemsT() throws Exception {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by IcaAtomEadSingleEad";

        int origCount = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, IcaAtomEadHandler.class).setTolerant(Boolean.TRUE).importFile(ios, logMessage);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnit
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 2 more import Event links
        // - 1 more import Event
        int createCount = origCount + 6;
        assertEquals(createCount, getNodeCount(graph));

        // Yet we've only created 1 *logical* item...
        assertEquals(1, log.getSuccessful());

        Iterable<Vertex> docs = graph.getVertices("identifier",
                IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);
        assertEquals("Test EAD Item", unit.getName());

        for(SystemEvent event : unit.getLatestEvent()){
            System.out.println("event: " + event.getLogMessage());
        }
        
        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Now re-import the same file
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log2 = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, IcaAtomEadHandler.class).importFile(ios2, logMessage);

        // We should only have three more nodes, for 
        // the action and 
        // the user event links, 
        // plus the global event
        assertEquals(createCount + 3, getNodeCount(graph));
        // And one logical item should've been updated
        assertEquals(1, log2.getUpdated());

    }
}
