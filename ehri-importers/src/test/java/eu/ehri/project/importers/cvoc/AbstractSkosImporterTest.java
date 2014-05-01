package eu.ehri.project.importers.cvoc;

import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AbstractSkosImporterTest extends AbstractFixtureTest {
    public static String FILE1 = "cvoc/simple.xml";
    public static String FILE2 = "cvoc/simple.n3";
    public static String FILE3 = "cvoc/repository-types.xml";
    public static String FILE4 = "cvoc/camps.rdf";
    public static String FILE5 = "cvoc/ghettos.rdf";

    protected Actioner actioner;
    protected Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actioner = manager.cast(validUser, Actioner.class);
        vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
    }
}
