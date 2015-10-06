/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import eu.ehri.project.importers.managers.SaxImportManager;
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
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.IOException;

public class ItsTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(ItsTest.class);
    protected final String EAD_EN = "exptestEsterwegen_en.xml"; //pertinence
    protected final String EAD_DE = "exptestEsterwegen_de.xml"; //pertinance
    protected final String GESTAPO = "its-gestapo-preprocessed.xml"; //provenance
    protected final String GESTAPO_WHOLE = "its-gestapo-whole.xml"; //provenance
    protected final String IMPORTED_ITEM_ID = "DE ITS [OuS 1.1.7]";
    
    DocumentaryUnit archdesc, c1, c2, c7_1, c7_2;

    @Test
    public void testUnitdate() throws Exception{
        InputStream ios = ClassLoader.getSystemResourceAsStream(EAD_EN);
         final String logMessage = "Importing a single EAD by ItsTest";
               importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its-pertinence.properties")).setTolerant(Boolean.TRUE);
        ImportLog log_en = importManager.importFile(ios, logMessage);
 DocumentaryUnit unit = graph.frame(
                getVertexByIdentifier(graph,IMPORTED_ITEM_ID),
                DocumentaryUnit.class);

       for(DocumentDescription desc : unit.getDocumentDescriptions()){
           for(String k : desc.asVertex().getPropertyKeys()){
               System.out.println(k + " - "+ desc.asVertex().getProperty(k));
           }
       }
    }
    @Test
    public void testItsImportEsterwegen() throws Exception {
        final String logMessage = "Importing a single EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(EAD_EN);
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(EAD_DE);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);


        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its-pertinence.properties")).setTolerant(Boolean.TRUE);
        ImportLog log_en = importManager.importFile(ios, logMessage);
        ImportLog log_de = importManager.importFile(ios2, logMessage);


// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
               printGraph(graph);
        diff.printDebug(System.out);


        /**
         * relationship: 2
         * null: 10 
         * documentaryUnit: 4 
         * documentDescription: 8 
         * maintenanceEvent: 8 (1+3)*2 
         * systemEvent: 2
         * date: 3 (2 ENG, 1 GER)
         */
        int createCount = origCount + 37;
        assertEquals(createCount, getNodeCount(graph));

        // The first import creates 4? units
        assertEquals(4, log_en.getCreated());

        // The second import does not create any units, but updates 4
        assertEquals(0, log_de.getCreated());
        assertEquals(4, log_de.getUpdated());

        Iterable<Vertex> docs = graph.getVertices("identifier", IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);

        assertEquals("nl-r1-de_its_ous_1_1_7", unit.getId());
        // The arch has 1 c01 direct child (and 2 c02 "grandchildren", but these are not counted)
        for (DocumentaryUnit d : unit.getChildren()) {
            logger.debug("Child: " + d.getIdentifier());
        }

        assertEquals(1L, unit.getChildCount());

        for (Description d : unit.getDocumentDescriptions()) {
            boolean hasDate=false;
            for(Edge e : d.asVertex().getEdges(Direction.OUT)){
                if(e.getLabel().equals("hasDate"))
                    hasDate=true;
            }
            assertTrue(hasDate);
            logger.debug("Description language: " + d.getLanguageOfDescription());
            if (d.getLanguageOfDescription().equals("eng")) {
                assertEquals("Concentration Camp Esterwegen", d.getName());
                assertTrue(d.asVertex().getProperty("extentAndMedium").toString().endsWith("draft by Susanne Laux"));
                
                assertEquals("2 folders\n\ndigitised\n\n7\n\nOriginals, Photocopies\n\ndraft by Susanne Laux", d.asVertex().getProperty("extentAndMedium"));

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
        assertEquals(repository, unit.getPermissionScope());
    }

    @Test
    public void testGestapo() throws ItemNotFound, IOException, ValidationError, InputParseError {
        final String logMessage = "Importing the gestapo (provenance) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(GESTAPO);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);


        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its-provenance.properties"))

                .setTolerant(Boolean.TRUE);
        ImportLog log_en = importManager.importFile(ios, logMessage);

// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

//               printGraph(graph);

        /* null: 9
         * relationship: 23
         * documentaryUnit: 8
         * property: 4
         * documentDescription: 8
         * maintenanceEvent: 4 (3 Revision + 1 Creation)
         * systemEvent: 1
         * datePeriod: 6
         */
//               printGraph(graph);
        int createCount = origCount + 63;
        assertEquals(createCount, getNodeCount(graph));
        
        DocumentaryUnit u = graph.frame(
                getVertexByIdentifier(graph, "R 2"), DocumentaryUnit.class);
        
            boolean foundOtherIdentifier = false;
            for(String s : (List<String>) u.asVertex().getProperty("otherIdentifiers")){
                if(s.equals("Folder 0143"))
                    foundOtherIdentifier=true;
            }
            assertTrue(foundOtherIdentifier);

            boolean foundDoc=false;
        for(DocumentDescription d : u.getDocumentDescriptions()){
            assertEquals("R 2 Geheime Staatspolizei (Gestapo).ead#DEU", d.asVertex().getProperty("sourceFileId"));
            
            assertTrue(((String) d.asVertex().getProperty("processInfo")).equals("ITS employee"));
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
        final String logMessage = "Importing the gestapo (provenance) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream(GESTAPO_WHOLE);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);


        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its-provenance.properties"))

                .setTolerant(Boolean.TRUE);
        ImportLog log_en = importManager.importFile(ios, logMessage);

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
    public void testEsterwegenWhole() throws ItemNotFound, IOException, ValidationError, InputParseError {
        final String logMessage = "Importing the esterwegen (pertinence) EAD by ItsTest";

        int origCount = getNodeCount(graph);


        InputStream ios = ClassLoader.getSystemResourceAsStream("esterwegen-whole.xml");
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("its-pertinence.properties"))

                .setTolerant(Boolean.TRUE);
        ImportLog log_en = importManager.importFile(ios, logMessage);

// After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
         printGraph(graph);

        diff.printDebug(System.out);
        /*
         * relationship: 1
         * null: 5
         * documentaryUnit: 4
         * documentDescription: 4
         * property: 1
         * maintenanceEvent: 4
         * systemEvent: 1
         * datePeriod: 2
         */

        int createCount = origCount + 21;
        assertEquals(createCount, getNodeCount(graph));
       
    }
}
