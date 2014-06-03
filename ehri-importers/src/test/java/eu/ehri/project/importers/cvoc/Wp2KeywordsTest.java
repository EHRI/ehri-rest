package eu.ehri.project.importers.cvoc;

import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author linda
 */
public class Wp2KeywordsTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(Wp2KeywordsTest.class);
    protected final String SKOS_FILE = "cvoc/wp2_skos_keywords.xml";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);
        log.printReport();

//        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 388 more Concepts
       	 * 368 more english ConceptDescription
         * 29 german
         * 381 czech
	 * 389 more import Event links (388 for every Unit, 1 for the User)
         * 1 more import Event
         */
        
        int afterNodeCount = count + 1556;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 388, toList(vocabulary.getConcepts()).size());

        Concept concept = graph.frame(getVertexByIdentifier(graph, "KEYWORD.JMP.847"), Concept.class);
        assertEquals("KEYWORD.JMP.847", concept.getIdentifier());
        for(Concept parent : concept.getBroaderConcepts()){
            assertEquals("KEYWORD.JMP.103", parent.getIdentifier());
        }

        Concept c103 = graph.frame(getVertexByIdentifier(graph, "KEYWORD.JMP.103"), Concept.class);
        boolean found847 = false;
        for(Concept child : c103.getNarrowerConcepts()){
            if(child.getIdentifier().equals("KEYWORD.JMP.847"))
                found847=true;
        }
        assertTrue(found847);
        
        // Check permission scopes
        for (AccessibleEntity e : log.getAction().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
    }
}

