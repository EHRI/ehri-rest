package eu.ehri.project.importers.cvoc;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import org.junit.Test;


import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.cvoc.Vocabulary;

/**
 *
 * @author linda
 */
public class SkosImporterAdminDistrictTest extends AbstractImporterTest {

    protected final String SINGLE_SKOS = "admin-dist-nolang.rdf";
//    protected final String SINGLE_SKOS = "admin-dist.rdf";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
  
    @Test
    public void testImportItemsT() throws Exception {
        UserProfile user = validUser; //graph.frame(graph.getVertex(validUserId), UserProfile.class);
        Repository agent = manager.getFrame(TEST_REPO, Repository.class); //graph.frame(helper.getTestVertex(TEST_REPO), Repository.class);

        final String logMessage = "Importing a single skos: " + SINGLE_SKOS;

        int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_SKOS);
//        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 3 more Concept
        // - 4 more ConceptDescription ( 3 de + 1 fr )
        // - 4 more ImportEvents ( 3 + 1 )
        // - 1 more import Action
        assertEquals(count + 12, getNodeCount(graph));

    }
}
