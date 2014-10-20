/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author linda
 */
public class YadVashemTest extends AbstractImporterTest{
    
       protected final String SINGLE_EAD = "YV_m19_eng.xml";
       protected final String SINGLE_EAD_HEB = "YV_m19_heb.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1",
            ARCHDESC = "M.19",
            C1 = "M.19/7",
            C2 = "M.19/7.1";
    
    protected final String SOURCE_FILE_ID="10660245#ENG";
    
    

    @Test
    public void testImportItemsT() throws Exception {

         Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        System.out.println(count);
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        XmlImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("yadvashem.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
       /*
        * null: 4
        * relationship: 5 (2 creator, 1 place, 1 subject, 1 geog)
        * documentaryUnit: 3
        * documentDescription: 3
        * property: 3
        * systemEvent: 1
        * datePeriod: 1
        */
       int newCount = count + 20;
        assertEquals(newCount, getNodeCount(graph));

        
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        Iterator<DocumentDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("eng", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_HEB);
        log = importManager.importFile(ios, logMessage);
//        printGraph(graph);
        
        i = c1.getDocumentDescriptions().iterator();
        nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
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
        int count_heb = getNodeCount(graph);
        
        System.out.println(count + " " + count + " " + count_heb);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_HEB);
        log = importManager.importFile(ios, logMessage);
        assertEquals(count_heb, getNodeCount(graph));
        
    }

}