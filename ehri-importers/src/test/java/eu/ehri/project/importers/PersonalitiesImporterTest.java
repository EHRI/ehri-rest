/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class PersonalitiesImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesImporterTest.class);
    protected final String SINGLE_EAD = "Personalities_small.csv";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItemsT() throws Exception {
        AuthoritativeSet authorativeSet = manager.getFrame("cvoc1", AuthoritativeSet.class);
        int voccount = toList(authorativeSet.getAuthoritativeItems()).size();
        logger.debug("number of items: " + voccount);
        
        final String logMessage = "Importing some WP18 Personalities records";
        XmlImportProperties p = new XmlImportProperties("personalities.properties");
        assertTrue(p.containsProperty("Othernames"));
        assertTrue(p.containsProperty("DateofbirthYYYY-MM-DD"));
        assertTrue(p.containsProperty("Pseudonyms"));
        

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, authorativeSet, validUser, PersonalitiesImporter.class).importFile(ios, logMessage);

        /*
         * 8 HistAgent
         * 8 HistAgentDesc
         * 9 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count+26, getNodeCount(graph));
        assertEquals(voccount+8, toList(authorativeSet.getAuthoritativeItems()).size());
       
    }
}
