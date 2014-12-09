/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
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
public class CsvConceptImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(CsvConceptImporterTest.class);
    protected final String SINGLE_EAD = "fast-subjects.csv";

    @Test
    public void testImportItemsT() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getFrame("auths", AuthoritativeSet.class);
        int voccount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, voccount);
        logger.debug("number of items: " + voccount);
        
        final String logMessage = "Importing some subjects";
        XmlImportProperties p = new XmlImportProperties("csvconcept.properties");
        assertTrue(p.containsProperty("id"));
        assertTrue(p.containsProperty("name"));
        assertTrue(p.containsProperty("url"));
        

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, authoritativeSet, validUser, CsvConceptImporter.class).importFile(ios, logMessage);
        System.out.println(Iterables.toList(authoritativeSet.getAuthoritativeItems()));
        /*
         * 18 Concept
         * 18 ConceptDesc
         * 19 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count+56, getNodeCount(graph));
        assertEquals(voccount + 18, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (AccessibleEntity subject : log.getAction().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
        Concept antisemitism = manager.getFrame("auths-fst810769", Concept.class);
        assertNotNull(antisemitism);
        for(Description d :  antisemitism.getDescriptions()){
            assertEquals("Antisemitism",d.getName());
            assertEquals("http://id.worldcat.org/fast/810769/", d.asVertex().getProperty("url"));
        }
    }
}
