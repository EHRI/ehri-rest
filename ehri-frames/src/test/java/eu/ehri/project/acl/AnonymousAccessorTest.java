package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Group;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AnonymousAccessorTest {
    @Test
    public void testIsAdmin() throws Exception {
        assertFalse(AnonymousAccessor.getInstance().isAdmin());
    }

    @Test
    public void testIsAnonymous() throws Exception {
        assertTrue(AnonymousAccessor.getInstance().isAnonymous());
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(
                Group.ANONYMOUS_GROUP_IDENTIFIER,
                AnonymousAccessor.getInstance().getId());
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(Entities.GROUP,
                AnonymousAccessor.getInstance().getType());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAsVertex() throws Exception {
        AnonymousAccessor.getInstance().asVertex();
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(
                Group.ANONYMOUS_GROUP_IDENTIFIER,
                AnonymousAccessor.getInstance().getIdentifier());
    }

    @Test
    public void testGetParents() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getParents()));
    }

    @Test
    public void testGetAllParents() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getAllParents()));
    }

    @Test
    public void testGetPermissionGrants() throws Exception {
        assertTrue(Iterables.isEmpty(AnonymousAccessor
                .getInstance().getPermissionGrants()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddPermissionGrant() throws Exception {
        AnonymousAccessor.getInstance().addPermissionGrant(null);
    }
}
