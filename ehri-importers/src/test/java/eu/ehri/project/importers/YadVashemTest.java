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
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author linda
 */
public class YadVashemTest extends AbstractImporterTest{
    
       protected final String SINGLE_EAD = "YV_m19_eng.xml";
       protected final String SINGLE_EAD_ENG = "YV_m19_heb.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1",
            ARCHDESC = "O.84",
            C1 = "O.84/199",
            C2 = "VAKKA-3058288.KA";
    
    

    @Test
    @Ignore
    public void testImportItemsT() throws Exception {

         Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        System.out.println(count);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        XmlImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("finlandead.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);
        int count_fin = getNodeCount(graph);
        
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        Iterator<DocumentDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("fin", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_ENG);
        log = importManager.importFile(ios, logMessage);
        printGraph(graph);
        
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
            //assertEquals("fin", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(2, nrOfDesc);
        int count_eng = getNodeCount(graph);
        
        System.out.println(count + " " + count_fin + " " + count_eng);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_ENG);
        log = importManager.importFile(ios, logMessage);
        assertEquals(count_eng, getNodeCount(graph));
        
    }

}
