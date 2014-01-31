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
 * @author linda
 */
public class Wp2PersonalitiesImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(Wp2PersonalitiesImporterTest.class);
    protected final String SINGLE_EAD = "wp2_persons_small.csv";

    @Test
    public void testImportItemsT() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getFrame("auths", AuthoritativeSet.class);
        int voccount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, voccount);
        logger.debug("number of items: " + voccount);
        
        final String logMessage = "Importing some WP2 Personalities records";
        XmlImportProperties p = new XmlImportProperties("wp2personalities.properties");
        assertTrue(p.containsProperty("id"));
        assertTrue(p.containsProperty("dateOfBirth"));
        assertTrue(p.containsProperty("name"));
        

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, authoritativeSet, validUser, Wp2PersonalitiesImporter.class).importFile(ios, logMessage);
        System.out.println(Iterables.toList(authoritativeSet.getAuthoritativeItems()));
        /*
         * 16 HistAgent
         * 16 HistAgentDesc
         * 9 more DatePeriods
         * 17 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        printGraph(graph);
        assertEquals(count+59, getNodeCount(graph));
        assertEquals(voccount + 16, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (AccessibleEntity subject : log.getAction().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
    }
}
