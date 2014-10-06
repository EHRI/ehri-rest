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

/**
 *
 * @author linda
 */
public class DansEadImporterTest extends AbstractImporterTest{
    
       protected final String SINGLE_EAD = "dans_convertedead_part.xml";

       // Depends on fixtures
    protected final String TEST_REPO ="r1",
            ARCHDESC = "easy-collection:2",
            C1 = "urn:nbn:nl:ui:13-4i8-gpf",
            SUBFONDS = "easy-collection:2:3",
            C2 = "urn:nbn:nl:ui:13-qa8-3r5";
    
    
    

    @Test
    public void testImportItemsT() throws Exception {

         Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        System.out.println(origCount);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        XmlImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("dansead.properties"))
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);
        printGraph(graph);
        /*
         * we should have
         * - 4 DocUnits
         * - 4 DocDesc
         * - 5 more import Event links (4 for every Unit, 1 for the User)
         * - 6 more Dates
         * - 6 more UndeterminedRelation
         * - 1 more import Event
         * - 1 MaintenaceEvent
         */
        int newCount = origCount + 14 + 6 + 6 + 1; 
        assertEquals(newCount, getNodeCount(graph));
        
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        Iterator<DocumentDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while(i.hasNext()){
            DocumentDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("nld", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);


        
    }

}
