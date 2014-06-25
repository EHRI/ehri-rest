package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class VirtualUnitTest extends AbstractFixtureTest {
    @Test
    public void testGetChildCount() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        assertEquals(Long.valueOf(1L), vc1.getChildCount());
    }

    @Test
    public void testGetParent() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        assertEquals(vc1, vu1.getParent());
    }

    @Test
    public void testAddChild() throws Exception {
        VirtualUnit vu2 = manager.getFrame("vu2", VirtualUnit.class);
        VirtualUnit vu3 = manager.getFrame("vu3", VirtualUnit.class);
        Long childCount = vu2.getChildCount();
        assertTrue(vu2.addChild(vu3));
        assertEquals(Long.valueOf(childCount + 1), vu2.getChildCount());
        // Doing the same thing twice should return false
        assertFalse(vu2.addChild(vu3));
    }

    @Test
    public void testAddChildWithBadChild() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        VirtualUnit vc1Alt = manager.getFrame("vc1", VirtualUnit.class);
        // This shouldn't be allowed!
        assertFalse(vc1.addChild(vc1));
        // Nor should this - loop
        assertFalse(vc1Alt.addChild(vc1));
    }

    @Test
    public void testGetReferencedDescriptions() throws Exception {
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        DocumentDescription cd1 = manager.getFrame("cd1", DocumentDescription.class);
        assertTrue(vu1.getReferencedDescriptions().iterator().hasNext());
        assertEquals(cd1, vu1.getReferencedDescriptions().iterator().next());
    }

    @Test
    public void testAddReferencedDescription() throws Exception {
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        DocumentDescription cd4 = manager.getFrame("cd4", DocumentDescription.class);
        assertEquals(1L, Iterables.size(vu1.getReferencedDescriptions()));
        vu1.addReferencedDescription(cd4);
        assertEquals(2L, Iterables.size(vu1.getReferencedDescriptions()));
        // check we can't add it twice
        vu1.addReferencedDescription(cd4);
        assertEquals(2L, Iterables.size(vu1.getReferencedDescriptions()));
    }

    @Test
    public void testGetVirtualDescriptions() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        DocumentDescription cd1 = manager.getFrame("vcd1", DocumentDescription.class);
        assertTrue(vc1.getVirtualDescriptions().iterator().hasNext());
        assertEquals(cd1, vc1.getVirtualDescriptions().iterator().next());
    }

    @Test
    public void testGetAncestors() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getFrame("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu1, vc1), Lists.newArrayList(vu2.getAncestors()));
    }

    @Test
    public void testGetChildren() throws Exception {
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getFrame("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu2), Lists.newArrayList(vu1.getChildren()));
    }

    @Test
    public void testGetAllChildren() throws Exception {
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        VirtualUnit vu2 = manager.getFrame("vu2", VirtualUnit.class);
        assertEquals(Lists.newArrayList(vu2), Lists.newArrayList(vu1.getAllChildren()));
    }

    @Test
    public void testGetAuthor() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        UserProfile linda = manager.getFrame("linda", UserProfile.class);
        assertEquals(linda, vc1.getAuthor());
    }

    @Test
    public void testSetAuthor() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        UserProfile linda = manager.getFrame("linda", UserProfile.class);
        Group kcl = manager.getFrame("kcl", Group.class);
        assertEquals(linda, vc1.getAuthor());
        vc1.setAuthor(kcl);
        assertEquals(kcl, vc1.getAuthor());
    }

    @Test
    public void testGetDescriptions() throws Exception {
        DocumentDescription cd1 = manager.getFrame("cd1", DocumentDescription.class);
        VirtualUnit vu1 = manager.getFrame("vu1", VirtualUnit.class);
        assertEquals(Lists.newArrayList(cd1), Lists.newArrayList(vu1.getReferencedDescriptions()));
    }

    @Test
    @Ignore //VirtualUnits do not belong to a Repository
    public void testGetRepositories() throws Exception {
        VirtualUnit vu2 = manager.getFrame("vu2", VirtualUnit.class);
        Repository r1 = manager.getFrame("r1", Repository.class);
//        assertEquals(Lists.newArrayList(r1), Lists.newArrayList(vu2.getRepositories()));
    }
}
