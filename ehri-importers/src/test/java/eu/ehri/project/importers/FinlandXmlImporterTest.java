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

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class FinlandXmlImporterTest extends AbstractImporterTest{
    
       protected final String SINGLE_EAD = "EHRI-test-ead-fin.xml";
       protected final String SINGLE_EAD_ENG = "EHRI-test-ead.xml";
    // Depends on fixtures
    protected final String
            C1 = "VAKKA-326611.KA",
            C2 = "VAKKA-3058288.KA";
    
    

    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        System.out.println(count);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("finlandead.properties"))
                .setTolerant(Boolean.TRUE);
        // Before...
//       List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);
 // After...
//       List<VertexProxy> graphState2 = getGraphState(graph);
//       GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);
        int count_fin = getNodeCount(graph);
        /**
         * null: 8
         * documentaryUnit: 7
         * documentDescription: 7
         * property: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 5
         */
        assertEquals(count+30, count_fin);
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        Iterator<DocumentDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("VAKKA-326611.KA#FIN", desc.asVertex().getProperty("sourceFileId"));
            assertEquals("fin", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);
        
        for(DocumentDescription dd : c2.getDocumentDescriptions()){
            assertEquals("VAKKA-326611.KA#FIN", dd.asVertex().getProperty("sourceFileId"));
        }
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        //import the english version:
        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_ENG);
        log = importManager.importFile(ios, logMessage);
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
//        printGraph(graph);
        /**
         * null: 8
         * property: 1
         * documentDescription: 7
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 5
         */
       assertEquals(count_fin + 23, getNodeCount(graph));
        i = c1.getDocumentDescriptions().iterator();
        nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            
            //sourceFileId with added languagetag:
            if(desc.getLanguageOfDescription().equals("eng")){
                assertEquals("VAKKA-326611.KA#ENG", desc.asVertex().getProperty("sourceFileId"));
            }else{
                assertEquals("VAKKA-326611.KA#FIN", desc.asVertex().getProperty("sourceFileId"));
            }
            //assertEquals("fin", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(2, nrOfDesc);
       
        i = c2.getDocumentDescriptions().iterator();
        nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(2, nrOfDesc);
        int count_eng = getNodeCount(graph);
// // Before...
       List<VertexProxy> graphState1a = getGraphState(graph);
        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_ENG);
        log = importManager.importFile(ios, logMessage);        
// // After...
       List<VertexProxy> graphState2a = getGraphState(graph);
       GraphDiff diffa = diffGraph(graphState1a, graphState2a);
       diffa.printDebug(System.out);
        
        System.out.println(count + " " + count_fin + " " + count_eng);

        /**
         * CREATED:
         * null: 2
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 1
         * 
         * REMOVED: 
         * maintenanceEvent: 1
         * datePeriod: 1
         */
        assertEquals(count_eng + (5-2), getNodeCount(graph));
        
    }

}
