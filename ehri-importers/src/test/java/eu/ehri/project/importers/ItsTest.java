package eu.ehri.project.importers;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;

public class ItsTest extends AbstractImporterTest {
	private static final Logger logger = LoggerFactory.getLogger(ItsTest.class);
    
	protected final String EAD_EN = "exptestEsterwegen_en.xml";
	protected final String EAD_DE = "exptestEsterwegen_de.xml";
    protected final String IMPORTED_ITEM_ID = "DE ITS [OuS 1.1.7]";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    DocumentaryUnit archdesc, c1, c2, c7_1, c7_2;
    int origCount=0;
	
	@Test
	public void testItsImport() throws Exception {
		Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by ItsTest";

        int origCount = getNodeCount(graph);

//        printGraph(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(EAD_EN);
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(EAD_DE);
        
        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, EadHandler.class)
                .setTolerant(Boolean.TRUE);
//        sim.importFiles(, logMessage)
        ImportLog log_en = sim.importFile(ios, logMessage);
        ImportLog log_de = sim.importFile(ios2, logMessage);
        

//        printGraph(graph);
        /* How many new nodes will have been created? We should have
        * 4 more DocumentaryUnit
        * 8 more DocumentDescription
        * ? more Date Period
        * ? more import Event links
        * ? more import Event
        * ? more UndeterminedRelationships
        * 2 more UnknownProperty, one in each documentDescription
        */
        int createCount = origCount + 24;
        assertEquals(createCount, getNodeCount(graph));

        // The first import creates 4? units
        assertEquals(4, log_en.getCreated());
        
        // The second import does not create any units, but updates 4
        assertEquals(0, log_de.getCreated());
        assertEquals(4, log_de.getUpdated());

        Iterable<Vertex> docs = graph.getVertices("identifier", IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);
        
        // The arch has 1 c01 direct child (and 2 c02 "grandchildren", but these are not counted)
        for (DocumentaryUnit d : unit.getChildren()) {
        	logger.debug("Child: " + d.getIdentifier());
        }
        assertEquals(new Long(1), unit.getChildCount());
        
        
        for(Description d : unit.getDocumentDescriptions()) {
        	logger.debug("Description language: " + d.getLanguageOfDescription());
        	if(d.getLanguageOfDescription().equals("en")) {
        		assertEquals("Concentration Camp Esterwegen", d.getName());
        	}
        	else if (d.getLanguageOfDescription().equals("de")){
        		assertEquals("Konzentrationslager Esterwegen", d.getName());
        	}
        }
            

        SystemEvent event = unit.getLatestEvent();
        if (event != null) {
            logger.debug("event: " + event.getLogMessage());
        }

        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Check scope is correct...
        assertEquals(agent, unit.getPermissionScope());
	}

}
