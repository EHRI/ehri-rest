package eu.ehri.project.importers;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.IOException;

public class ItsTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(ItsTest.class);
    protected final String EAD_EN = "exptestEsterwegen_en.xml";
    protected final String EAD_DE = "exptestEsterwegen_de.xml";
    protected final String GESTAPO = "its-gestapo-preprocessed.xml";
    protected final String GESTAPO_WHOLE = "its-gestapo-whole.xml";
    protected final String IMPORTED_ITEM_ID = "DE ITS [OuS 1.1.7]";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    DocumentaryUnit archdesc, c1, c2, c7_1, c7_2;

    @Test
    public void testItsImportEsterwegen() throws Exception {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(EAD_EN);
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(EAD_DE);
        // Before...
//        List<VertexProxy> graphState1 = getGraphState(graph);

        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its.properties")).setTolerant(Boolean.TRUE);
        ImportLog log_en = sim.importFile(ios, logMessage);
        ImportLog log_de = sim.importFile(ios2, logMessage);

// After...
//        List<VertexProxy> graphState2 = getGraphState(graph);
//        GraphDiff diff = diffGraph(graphState1, graphState2);
//        diff.printDebug(System.out);

//               printGraph(graph);

        /**
         * null: 10 
         * documentaryUnit: 4 
         * documentDescription: 8 
         * property: 8 
         * maintenanceEvent: 8 (1+3)*2 
         * systemEvent: 2
         */
        int createCount = origCount + 40;
        assertEquals(createCount, getNodeCount(graph));

        // The first import creates 4? units
        assertEquals(4, log_en.getCreated());

        // The second import does not create any units, but updates 4
        assertEquals(0, log_de.getCreated());
        assertEquals(4, log_de.getUpdated());

        Iterable<Vertex> docs = graph.getVertices("identifier", IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);

        assertEquals("nl-r1-de-its-ous-1-1-7-", unit.getId());
        // The arch has 1 c01 direct child (and 2 c02 "grandchildren", but these are not counted)
        for (DocumentaryUnit d : unit.getChildren()) {
            logger.debug("Child: " + d.getIdentifier());
        }
        assertEquals(new Long(1), unit.getChildCount());


        for (Description d : unit.getDocumentDescriptions()) {
            logger.debug("Description language: " + d.getLanguageOfDescription());
            if (d.getLanguageOfDescription().equals("eng")) {
                assertEquals("Concentration Camp Esterwegen", d.getName());

            } else if (d.getLanguageOfDescription().equals("deu")) {
                assertEquals("Konzentrationslager Esterwegen", d.getName());
            } else {
                fail();
            }
        }


        SystemEvent event = unit.getLatestEvent();
        if (event != null) {
            logger.debug("event: " + event.getLogMessage());
        }

        List<SystemEvent> actions = toList(unit.getHistory());
        // we did two imports, so two actions
        assertEquals(2, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Check scope is correct...
        assertEquals(agent, unit.getPermissionScope());
    }

    @Test
    public void testGestapo() throws ItemNotFound, IOException, ValidationError, InputParseError {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing the gestapo (provenance) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(GESTAPO);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log_en = sim.importFile(ios, logMessage);

// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

               printGraph(graph);

        /* null: 9
         * documentaryUnit: 8
         * property: 8
         * documentDescription: 8
         * maintenanceEvent: 4 (3 Revision + 1 Creation)
         * systemEvent: 1
         * datePeriod: 2
         */
        int createCount = origCount + 40;
        assertEquals(createCount, getNodeCount(graph));
        
        DocumentaryUnit u = graph.frame(
                getVertexByIdentifier(graph, "R2 Gestapo_0"), DocumentaryUnit.class);
        boolean foundDoc=false;
        for(DocumentDescription d : u.getDocumentDescriptions()){
            assertEquals("R2 Gestapo#DEU", d.asVertex().getProperty("sourceFileId"));
            
            boolean authorInProcessInfo = false;
            for(String processInfo : (List<String>) d.asVertex().getProperty("processInfo")){
                authorInProcessInfo = authorInProcessInfo || processInfo.equals("ITS employee");
            }
            assertTrue(authorInProcessInfo);
            foundDoc=true;

            int countRevised_ME=0;
            int countCreated_ME=0;
            for (MaintenanceEvent me : d.getMaintenanceEvents()) {
                if (me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE).equals(MaintenanceEvent.EventType.REVISED.toString())) {
                    assertNotNull(me.asVertex().getProperty("source"));
                    assertNotNull(me.asVertex().getProperty("date"));
                    assertNotNull(me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE));
                    assertEquals(MaintenanceEvent.EventType.REVISED.toString(), me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE));
                    countRevised_ME++;
                } else if (me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE).equals(MaintenanceEvent.EventType.CREATED.toString())) {
                    assertNotNull(me.asVertex().getProperty("source"));
                    assertNull(me.asVertex().getProperty("date"));
                    assertNotNull(me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE));
                    assertEquals(MaintenanceEvent.EventType.CREATED.toString(), me.asVertex().getProperty(MaintenanceEvent.EVENTTYPE));
                    countCreated_ME++;
                }

                
            }
            assertEquals(3, countRevised_ME);
            assertEquals(1, countCreated_ME);
        }
        assertTrue(foundDoc);
        
        

    }

    @Test
    @Ignore
    public void testGestapoWhole() throws ItemNotFound, IOException, ValidationError, InputParseError {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing the gestapo (provenance) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(GESTAPO_WHOLE);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log_en = sim.importFile(ios, logMessage);

// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

//               printGraph(graph);

        /* null: 21
         * documentaryUnit: 20
         * property: 20
         * documentDescription: 20
         * maintenanceEvent: 3
         * systemEvent: 1
         * datePeriod: 6
         */
        int createCount = origCount + 91;
        assertEquals(createCount, getNodeCount(graph));

    }

    @Test
    @Ignore
    public void testEsterwegenWhole() throws ItemNotFound, IOException, ValidationError, InputParseError {
        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing the esterwegen (pertinence) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream("esterwegen-whole.xml");
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        XmlImportManager sim = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log_en = sim.importFile(ios, logMessage);

// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        int createCount = origCount + 21;
        assertEquals(createCount, getNodeCount(graph));

    }
}
