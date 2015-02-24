/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class YadVashemTest extends AbstractImporterTest{
    private static final Logger logger = LoggerFactory.getLogger(YadVashemTest.class);
       protected final String SINGLE_EAD = "YV_m19_eng.xml";
       protected final String SINGLE_EAD_HEB = "YV_m19_heb.xml";
       protected final String SINGLE_EAD_C1 = "YV_c1.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1",
            ARCHDESC = "M.19",
            C1 = "M.19/7",
            C2 = "M.19/7.1";
    
    protected final String SOURCE_FILE_ID="10660245#ENG";
    

    @Test
    public void testWithExistingDescription() throws Exception{
        final String logMessage = "Importing a single EAD";
        DocumentaryUnit m19 = manager.getFrame("nl-r1-m19", DocumentaryUnit.class);
        
        assertEquals("m19", m19.getIdentifier());
        assertEquals(1, toList(m19.getDocumentDescriptions()).size());

        int count = getNodeCount(graph);
        System.out.println(count);
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_C1);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("yadvashem.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
       /**
        * null: 4
        * relationship: 4
        * documentaryUnit: 2
        * documentDescription: 3
        * systemEvent: 1
        * datePeriod: 1
        */

        assertEquals(count + 15, getNodeCount(graph));
        assertEquals(2, toList(m19.getDocumentDescriptions()).size());
    }

    @Test 
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        System.out.println(count);
        DocumentaryUnit m19 = manager.getFrame("nl-r1-m19", DocumentaryUnit.class);
        
        assertEquals("m19", m19.getIdentifier());
        assertEquals(1, toList(m19.getDocumentDescriptions()).size());
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("yadvashem.properties"))
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
        * documentaryUnit: 2
        * documentDescription: 3
        * property: 1
        * systemEvent: 1
        * datePeriod: 1
        */
        assertEquals(count + 17, getNodeCount(graph));
        //ENG also imported:
assertEquals(2, toList(m19.getDocumentDescriptions()).size());
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
        //HEB also imported:
        assertEquals(3, toList(m19.getDocumentDescriptions()).size());
        logger.debug("size: "+ toList(m19.getDocumentDescriptions()).size());
        for(DocumentDescription m19desc : m19.getDocumentDescriptions()){
            logger.debug(m19desc.getId() + ":" + m19desc.getLanguageOfDescription() + ":" + m19desc.asVertex().getProperty(Ontology.SOURCEFILE_KEY));
        }
        
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
        printGraph(graph);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_HEB);
                 // Before...
       List<VertexProxy> graphState1_heb = getGraphState(graph);
        logger.debug("reimport HEB");
        log = importManager.importFile(ios, logMessage);
                // After...
       List<VertexProxy> graphState2_heb = getGraphState(graph);
       GraphDiff diff_heb = diffGraph(graphState1_heb, graphState2_heb);
       diff_heb.printDebug(System.out);
        logger.debug("reimport HEB");
        //HEB re imported:
        assertEquals(3, toList(m19.getDocumentDescriptions()).size());
        assertEquals(count_heb, getNodeCount(graph));
        
    }

}
