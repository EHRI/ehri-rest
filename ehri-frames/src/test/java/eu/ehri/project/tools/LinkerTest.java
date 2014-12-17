package eu.ehri.project.tools;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LinkerTest extends AbstractFixtureTest {

    private Linker linker;
    private ActionManager actionManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        linker = new Linker(graph);
        actionManager = new ActionManager(graph);
    }

    @Test
    public void testCreateConceptsForRepository() throws Exception {
        Repository repository = manager.getFrame("r1", Repository.class);
        Vocabulary vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
        int eventCount = Iterables.size(actionManager.getLatestGlobalEvents());
        // The doc c1 contains two access point nodes which should be made
        // into links
        long newLinkCount = linker.createAndLinkRepositoryVocabulary(repository, vocabulary, validUser,
                "eng", Lists.<String>newArrayList(),
                Optional.of("This is a test!"), false);
        assertEquals(2, newLinkCount);
        assertEquals(eventCount + 2,
                Iterables.size(actionManager.getLatestGlobalEvents()));
    }

    @Test
    public void testCreateConceptsForRepositoryWithBadAccessPointTypes() throws Exception {
        Repository repository = manager.getFrame("r1", Repository.class);
        Vocabulary vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
        int eventCount = Iterables.size(actionManager.getLatestGlobalEvents());

        // This won't create any links because the access point type
        // won't match any existing access points.
        long newLinkCount = linker.createAndLinkRepositoryVocabulary(repository, vocabulary, validUser,
                "eng", Lists.<String>newArrayList("notAValidAccessPoint"), Optional.<String>absent(), false);
        assertEquals(0, newLinkCount);
        // No new events should have been created...
        assertEquals(eventCount,
                Iterables.size(actionManager.getLatestGlobalEvents()));
    }
}
