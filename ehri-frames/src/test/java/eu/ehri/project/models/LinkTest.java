package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LinkTest extends AbstractFixtureTest {
    @Test
    public void testGetLinker() throws Exception {
        Link link = manager.getFrame("link1", Link.class);
        assertEquals(validUser, link.getLinker());
    }

    @Test
    public void testGetLinkTargets() throws Exception {
        Link link = manager.getFrame("link1", Link.class);
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        assertTrue(Iterables.contains(link.getLinkTargets(), c1));
        assertTrue(Iterables.contains(link.getLinkTargets(), c4));
    }

    @Test
    public void testGetLinkBodies() throws Exception {
        Link link = manager.getFrame("link2", Link.class);
        UndeterminedRelationship ur1 = manager.getFrame("ur1", UndeterminedRelationship.class);
        assertTrue(Iterables.contains(link.getLinkBodies(), ur1));
    }
}
