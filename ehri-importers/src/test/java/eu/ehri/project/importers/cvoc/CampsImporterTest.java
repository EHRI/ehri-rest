package eu.ehri.project.importers.cvoc;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.views.Query;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author linda
 */
public class CampsImporterTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(CampsImporterTest.class);
    protected final String SKOS_FILE = "camps.rdf";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        JenaVocabularyImporter importer = new JenaVocabularyImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);
        log.printReport();

        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 8 more Concepts
       	 * 8 more ConceptDescription
	     * 9 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        int afterNodeCount = count + 26;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 8, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "675";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);
        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).list(
                Ontology.IDENTIFIER_KEY, skosConceptId, validUser));

        assertEquals(1, toList(list.get(0).getBroaderConcepts()).size());

        // Check permission scopes
        for (AccessibleEntity e : log.getAction().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
    }
}
