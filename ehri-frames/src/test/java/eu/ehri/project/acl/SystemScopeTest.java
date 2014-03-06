package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SystemScopeTest {
    @Test
    public void testGetId() throws Exception {
        assertEquals(Entities.SYSTEM,
                SystemScope.getInstance().getId());
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(Entities.SYSTEM,
                SystemScope.getInstance().getType());
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals(Entities.SYSTEM, SystemScope.getInstance().getType());
    }

    @Test
    public void testAsVertex() throws Exception {
        assertNull(SystemScope.getInstance().asVertex());
    }

    @Test
    public void testGetPermissionGrants() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getPermissionGrants()));
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getPermissionScopes()));
    }

    @Test
    public void testGetContainedItems() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getContainedItems()));
    }

    @Test
    public void testGetAllContainedItems() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope
                .getInstance().getAllContainedItems()));
    }

    @Test
    public void testIdPath() throws Exception {
        assertTrue(Iterables.isEmpty(SystemScope.getInstance().idPath()));
    }
}
