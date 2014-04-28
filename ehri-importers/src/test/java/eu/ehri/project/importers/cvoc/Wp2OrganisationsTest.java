/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.cvoc;

import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.cvoc.Vocabulary;
import java.io.InputStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class Wp2OrganisationsTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(Wp2KeywordsTest.class);
    protected final String SKOS_FILE = "wp2_skos_organisations.rdf";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);
    }
    
}
