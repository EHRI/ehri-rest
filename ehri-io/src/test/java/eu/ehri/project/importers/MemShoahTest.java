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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the import of a memorial de la shoah EAD file.
 */
public class MemShoahTest extends AbstractImporterTest{
    
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "memshoah.xml";
    protected final String ARCHDESC = "II, V, VI, VIa";
    DocumentaryUnit archdesc;
    int origCount=0;
            
    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example MemShoah EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("memshoah.properties")).importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
        
//        printGraph(graph);
        // How many new nodes will have been created? We should have
        /** 
         * relationship: 1
         * events: 2
         * documentaryUnit: 1
         * documentDescription: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 1
         */ 

        int newCount = origCount + 8;
        assertEquals(newCount, getNodeCount(graph));
        
        archdesc = graph.frame(
                getVertexByIdentifier(graph,ARCHDESC),
                DocumentaryUnit.class);
        
        for (DocumentDescription d : archdesc.getDocumentDescriptions()) {
            assertEquals("Ambassade d'Allemagne (German Embassy)", d.getName());
            assertEquals("eng", d.getLanguageOfDescription());
        }

    
        //test MaintenanceEvent order
        for(DocumentDescription dd : archdesc.getDocumentDescriptions()){

            boolean meFound = false;
            int countME=0;
            for(MaintenanceEvent me : dd.getMaintenanceEvents()){
                meFound=true;
                countME++;
                if(me.getProperty("order").equals(0)){
                    assertEquals(MaintenanceEventType.created.toString(), me.getProperty("eventType"));
                }else{
                    assertEquals(MaintenanceEventType.updated.toString(), me.getProperty("eventType"));
                }
            }
            assertTrue(meFound);
            assertEquals(1, countME);
        }
        
        
        // Fonds has two dates with different types -> list
        for(DocumentDescription d : archdesc.getDocumentDescriptions()){
        	// start and end dates correctly parsed and setup
        	List<DatePeriod> dp = toList(d.getDatePeriods());
        	assertEquals(1, dp.size());
        	assertEquals("1939-01-01", dp.get(0).getStartDate());
        	assertEquals("1943-12-31", dp.get(0).getEndDate());
        }
        
    }
}
