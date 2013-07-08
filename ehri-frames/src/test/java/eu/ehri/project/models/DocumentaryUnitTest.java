package eu.ehri.project.models;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DocumentaryUnitTest extends AbstractFixtureTest {

    @Test
    public void testCollectionHelpByRepo() throws ItemNotFound {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        assertNotNull(unit.getRepository());
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testChildDocsCanAccessTheirAgent() throws ItemNotFound {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getFrame("c3", DocumentaryUnit.class);
        assertNotNull(child.getRepository());
        assertNotNull(unit.getRepository());
        assertEquals(unit.getRepository(), child.getRepository());
    }
}
