/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.*;

import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class PersonalitiesImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesImporterTest.class);
    protected final String SINGLE_EAD = "Personalities_small.csv";

    @Test
    public void testImportItemsT() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getFrame("auths", AuthoritativeSet.class);
        int voccount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, voccount);
        logger.debug("number of items: " + voccount);
        
        final String logMessage = "Importing some WP18 Personalities records";
        XmlImportProperties p = new XmlImportProperties("personalities.properties");
        assertTrue(p.containsProperty("Othernames"));
        assertTrue(p.containsProperty("DateofbirthYYYY-MM-DD"));
        assertTrue(p.containsProperty("Pseudonyms"));
        

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, authoritativeSet, validUser, PersonalitiesImporter.class).importFile(ios, logMessage);
        System.out.println(Iterables.toList(authoritativeSet.getAuthoritativeItems()));
        /*
         * 8 HistAgent
         * 8 HistAgentDesc
         * 8 more DatePeriods
         * 9 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count+34, getNodeCount(graph));
        assertEquals(voccount + 8, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (AccessibleEntity subject : log.getAction().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
    }
}
