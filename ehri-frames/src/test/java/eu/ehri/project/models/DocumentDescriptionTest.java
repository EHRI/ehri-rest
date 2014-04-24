package eu.ehri.project.models;

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DocumentDescriptionTest extends AbstractFixtureTest {
    @Test
    public void testGetVirtualCollections() throws Exception {
        VirtualUnit vc1 = manager.getFrame("vc1", VirtualUnit.class);
        DocumentDescription cd1 = manager.getFrame("cd1", DocumentDescription.class);
        DocumentDescription cd2 = manager.getFrame("cd2", DocumentDescription.class);
        // Not in a VC
        DocumentDescription cd4 = manager.getFrame("cd4", DocumentDescription.class);
        assertTrue(cd1.getVirtualCollections().iterator().hasNext());
        assertEquals(vc1, cd1.getVirtualCollections().iterator().next());
        assertTrue(cd2.getVirtualCollections().iterator().hasNext());
        assertEquals(vc1, cd2.getVirtualCollections().iterator().next());
        assertFalse(cd4.getVirtualCollections().iterator().hasNext());
    }
}
