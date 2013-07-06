/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.Country;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class UkrainianRepoImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(UkrainianRepoImporterTest.class);
    protected final String SINGLE_EAD = "ukraine_small.csv";
    protected final String TEST_COUNTRY = "r1";

    @Test
    public void testImportItemsT() throws Exception {

        int count = getNodeCount(graph);

        final String logMessage = "Importing some Ukrainian records";
        XmlImportProperties p = new XmlImportProperties("ukraine_repo.properties");
        assertTrue(p.containsProperty("Authorized_forms_of_name"));
        assertTrue(p.containsProperty("repository_code"));
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        Country country = manager.getFrame(TEST_COUNTRY, Country.class);

        ImportLog log = new CsvImportManager(graph, country, validUser, UkrainianRepoImporter.class).importFile(ios, logMessage);

        /*
         * 1 Repository Unit
         * 1 Repository Description
         * 16 updates
         * 2 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         * (==21)
         * 
         * plus the nodes from the UnitImporter:
         * 17 DocumentaryUnits
         * 17 + 3 DocumentsDescription (there are 3 desc's with 2 languages)
         * 17 + 3 DatePeriods
         * 18 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         * (==96)
         * 
         * minus 
         * 1 import Event link (for the User)
         * 1 import Event
         * 17 import Event links (for the Documentary Units, as they are now imported as part of the RepoUnit)
         */
        graph.getBaseGraph().commit();

        printGraph(graph);
        assertEquals(count+21+76-2-17, getNodeCount(graph));
//        assertEquals(voccount + 8, toList(authoritativeSet.getAuthoritativeItems()).size());
       
    }
}
