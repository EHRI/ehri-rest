package eu.ehri.project.models.base;

import com.google.common.collect.Lists;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AccessorTest extends AbstractFixtureTest {
    @Test
    public void testIsAdmin() throws Exception {

    }

    @Test
    public void testIsAnonymous() throws Exception {

    }

    @Test
    public void testGetParents() throws Exception {
        UserProfile user = manager.getFrame("tim", UserProfile.class);
        List<Accessor> accessors = Lists.newArrayList(user.getParents());
        assertEquals(1, accessors.size());
        assertEquals(manager.getFrame("niod", Group.class), accessors.get(0));
    }

    @Test
    public void testGetAllParents() throws Exception {
        UserProfile user = manager.getFrame("tim", UserProfile.class);
        List<Accessor> accessors = Lists.newArrayList(user.getAllParents());
        assertEquals(2, accessors.size());
        assertTrue(accessors.contains(manager.getFrame("niod", Group.class)));
        assertTrue(accessors.contains(manager.getFrame("admin", Group.class)));
    }
}
