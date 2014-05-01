package eu.ehri.project.importers.cvoc;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JenaSkosImporterTest extends AbstractFixtureTest {
    public static String FILE1 = "cvoc/simple.xml";
    public static String FILE2 = "cvoc/simple.n3";

    private Actioner actioner;
    private Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actioner = manager.cast(validUser, Actioner.class);
        vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
    }


    @Test
    public void testImportFile1() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE1), "simple 1");
        assertEquals(1, importLog.getCreated());
    }

    @Test
    public void testImportFile2() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        importer.setFormat("N3");
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE2), "simple 2");
        assertEquals(1, importLog.getCreated());
    }
}
