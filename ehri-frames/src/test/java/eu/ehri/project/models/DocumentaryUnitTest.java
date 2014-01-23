package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void testCannotAddChildOfRelationshipTwice() throws Exception {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getFrame("c2", DocumentaryUnit.class);
        assertEquals(unit, child.getParent());
        assertEquals(Long.valueOf(1L), unit.getChildCount());
        unit.addChild(child);
        assertEquals(Long.valueOf(1L), unit.getChildCount());
    }

    @Test
    public void testCannotAddSelfAsChild() throws Exception {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        assertEquals(Long.valueOf(1L), unit.getChildCount());
        unit.addChild(unit);
        assertEquals(Long.valueOf(1L), unit.getChildCount());
    }

    @Test
    public void testParentChildRelationship() throws ItemNotFound {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getFrame("c2", DocumentaryUnit.class);
        DocumentaryUnit child2 = manager.getFrame("c3", DocumentaryUnit.class);
        assertEquals(unit, child.getParent());
        assertEquals(child, child2.getParent());
        assertTrue(Iterables.contains(unit.getChildren(), child));
        assertTrue(Iterables.contains(unit.getAllChildren(), child));
        assertTrue(Iterables.contains(unit.getAllChildren(), child2));
        assertTrue(Iterables.contains(unit.getAllChildren(), child2));
    }
}
