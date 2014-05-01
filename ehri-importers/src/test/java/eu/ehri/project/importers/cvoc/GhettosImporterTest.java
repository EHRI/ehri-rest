package eu.ehri.project.importers.cvoc;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.views.Query;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author linda
 */
public class GhettosImporterTest extends AbstractImporterTest {
    protected final String SKOS_FILE = "cvoc/ghettos.rdf";

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
        //printGraph(graph);

        /*  How many new nodes will have been created? We should have
         * 2 more Concepts
         * 4 more ConceptDescription
         * 4 UndeterminedRelationships
     	 * 3 more import Event links (2 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 14, getNodeCount(graph));
        assertEquals(2, log.getCreated());
        assertEquals(voccount + 2, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "0";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);

        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).list(
                Ontology.IDENTIFIER_KEY, skosConceptId, validUser));

        Concept ghetto0 = list.get(0);
        //  <geo:lat>52.43333333333333</geo:lat>
        //	<geo:long>20.716666666666665</geo:long>
        Iterable<Description> ghetto0desc = ghetto0.getDescriptions();
        for (Description d : ghetto0desc) {
            assertEquals("52.43333333333333", d.asVertex().getProperty("latitude"));
            assertEquals("20.716666666666665", d.asVertex().getProperty("longitude"));
        }

        // and print the tree
        printConceptTree(System.out, list.get(0));
    }
}
