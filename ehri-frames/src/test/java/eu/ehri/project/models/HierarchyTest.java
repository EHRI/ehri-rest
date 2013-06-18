package eu.ehri.project.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import eu.ehri.project.test.ModelTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.DocumentaryUnit;

public class HierarchyTest extends ModelTestBase {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testCollectionHierarchy() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getFrame("c2", DocumentaryUnit.class);
        assertTrue(toList(c1.getChildren()).contains(c2));

        // check reverse
        assertEquals(c2.getParent(), c1);
    }

    @Test
    public void testCollectionAncestry() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getFrame("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getFrame("c3", DocumentaryUnit.class);
        // should be the first ancestor of c2
        assertEquals(toList(c2.getAncestors()).get(0), (c1));

        // and an ancestor of c3
        assertTrue(toList(c3.getAncestors()).contains(c1));
    }

    @Test
    public void testFullAncestry() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getFrame("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = manager.getFrame("c3", DocumentaryUnit.class);

        List<DocumentaryUnit> ancestors = toList(c3.getAncestors());
        assertEquals(2, ancestors.size());
        assertEquals(ancestors.get(0), c2);
        assertEquals(ancestors.get(1), c1);
    }
}
