/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class UshmmTest extends AbstractImporterTest{
    private static final Logger logger = LoggerFactory.getLogger(UshmmTest.class);
    
    protected final String SINGLE_EAD = "irn50845.xml";
    protected final String IMPORTED_ITEM_ID = "50845";
    protected final String IMPORTED_ITEM_ALT_ID = "RG-50.586*0138";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItemsT() throws Exception {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by UshmmTest";

        int origCount = getNodeCount(graph);

        printGraph(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, UshmmHandler.class)
                .setTolerant(Boolean.TRUE).importFile(ios, logMessage);

        printGraph(graph);
        /* How many new nodes will have been created? We should have
        * 1 more DocumentaryUnit
        * 1 more DocumentDescription
        * 1 more Date Period
        * 2 more import Event links
        * 1 more import Event
        * 5 more UndeterminedRelationships
        * 1 more UnknownProperty
        */
        int createCount = origCount + 9;
        assertEquals(createCount, getNodeCount(graph));

        // Yet we've only created 1 *logical* item...
        assertEquals(1, log.getChanged());

        Iterable<Vertex> docs = graph.getVertices("identifier", IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);
        for(Description d : unit.getDocumentDescriptions()) {
            assertEquals("Oral history interview with Marijan Perić", d.getName());
        	assertEquals("eng", d.getLanguageOfDescription());
        }
        SystemEvent event = unit.getLatestEvent();
        if (event != null) {
            logger.debug("event: " + event.getLogMessage());
        }
        
        // Check the alternative ID was added
        boolean foundAltId = false;
        for(String altId : (List<String>) unit.asVertex().getProperty("otherIdentifiers")) {
        	if (altId.equals(IMPORTED_ITEM_ALT_ID)) {
        		foundAltId = true;
        		break;
        	}
        }
        assertTrue(foundAltId);

        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Check scope is correct...
        assertEquals(agent, unit.getPermissionScope());

//        // Now re-import the same file
//        InputStream ios2 = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
//        ImportLog log2 = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, BundesarchiveEadHandler.class).importFile(ios2, logMessage);
//
//        // We should only have three more nodes, for 
//        // the action and 
//        // the user event links, 
//        // plus the global event
//        assertEquals(createCount + 3, getNodeCount(graph));
//        // And one logical item should've been updated
//        assertEquals(1, log2.getUpdated());

    }

}
