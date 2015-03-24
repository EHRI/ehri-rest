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
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the import of a Cegesoma AB EAD file.
 * This file was based on BundesarchiveTest.java.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Ben Companjen (http://github.com/bencomp)
 */
public class CegesomaABTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "CegesomaAB.xml";
    protected final String ARCHDESC = "AB 2029";
    DocumentaryUnit archdesc;
    int origCount=0;
            
    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Cegesoma EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("cegesomaAB.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);
        
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        /** 
         * event links: 2
         * relationship: 3
         * documentaryUnit: 1
         * documentDescription: 1
         * unknown property: 1
         * systemEvent: 1
         * datePeriod: 1
         * maintenanceEvent: 7
         */
        int newCount = origCount + 17;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        
        //José Gotovitch
        for (DocumentDescription d : archdesc.getDocumentDescriptions()) {
            boolean hasPersonAccess = false;
            for (Vertex relation : d.asVertex().getVertices(Direction.OUT, Ontology.HAS_ACCESS_POINT)) {
                Link link = graph.frame(relation, Link.class);
                if (link.getLinkType().equals("creatorAccess")) {
                    hasPersonAccess = true;
                    assertEquals("José Gotovitch", link.asVertex().getProperty(Ontology.NAME_KEY));
                }
            }
            assertTrue(hasPersonAccess);
        }

        // There should be one DocumentDescription for the <archdesc>
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){
            assertEquals("Liste des objets, documents et témoignages rassemblés pour l'exposition : (\"Résister à la solution finale\")", dd.getName());
            assertEquals("fra", dd.getLanguageOfDescription());
            assertEquals("Cege Soma", dd.asVertex().getProperty("processInfo"));
            assertEquals("en français et en anglais", dd.asVertex().getProperty("languageOfMaterial"));
        }
        
        //test MaintenanceEvent order
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){

            boolean meFound = false;
            for(MaintenanceEvent me : dd.getMaintenanceEvents()){
                meFound=true;
                if(me.asVertex().getProperty("order").equals(0)){
                    assertEquals(MaintenanceEvent.EventType.CREATED.toString(), me.asVertex().getProperty("eventType"));
                }else{
                    assertEquals(MaintenanceEvent.EventType.REVISED.toString(), me.asVertex().getProperty("eventType"));
                }
            }
            assertTrue(meFound);
        }
        
        
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(1, dp.size());
        	assertEquals("1940-01-01", dp.get(0).getStartDate());
        	assertEquals("1945-12-31", dp.get(0).getEndDate());
        }
        
    }
}
