package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.junit.Test;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.SkosCoreCvocImporter;
import eu.ehri.project.test.AbstractFixtureTest;

public class SkosCoreCvocImporterTest extends AbstractFixtureTest {
    protected final String SKOS_FILE = "skos.rdf";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        // How many new nodes will have been created? We should have
        
        // Yet we've only created 1 *logical* item...
        //assertEquals(1, log.getSuccessful());   
    }

}
