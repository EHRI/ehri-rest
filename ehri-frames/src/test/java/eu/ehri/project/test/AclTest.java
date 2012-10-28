package eu.ehri.project.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

public class AclTest extends ModelTestBase {

    private AclManager acl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        acl = new AclManager(graph);
    }

    @Test
    public void testTheFixturesLoaded() {
        assertTrue(graph.getVertices().iterator().hasNext());
    }

    @Test
    public void testTheAdminGroup() {
        Group admin = helper.getTestFrame("adminGroup", Group.class);
        // check we have some users
        assertTrue(admin.getUsers().iterator().hasNext());
    }

    /**
     * Ensure user 'Reto' can't access collection 'c3'.
     */
    @Test
    public void testAdminRead() {
        Group admin = helper.getTestFrame("adminGroup", Group.class);
        UserProfile reto = helper.getTestFrame("reto", UserProfile.class);
        DocumentaryUnit c3 = helper.getTestFrame("c3", DocumentaryUnit.class);
        assertTrue(acl.getAccessControl(c3, admin));
        assertFalse(acl.getAccessControl(c3, reto));
    }

    /**
     * Test NIOD group has no access to items with admin perms.
     */
    @Test
    public void testNiodGroup() {
        Group niod = helper.getTestFrame("niodGroup", Group.class);
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        assertFalse(acl.getAccessControl(c1, niod));

        // but we should have read-only access to items with no specified perms.
        DocumentaryUnit c4 = helper.getTestFrame("c4", DocumentaryUnit.class);
        assertTrue(acl.getAccessControl(c4, niod));
    }

    /**
     * Group permissions override user permissions.
     */
    @Test
    public void testUserGroupPermOverride() {
        Accessor tim = helper.getTestFrame("tim", Accessor.class);
        AccessibleEntity c3 = helper.getTestFrame("c3", AccessibleEntity.class);
        assertTrue(acl.getAccessControl(c3, tim));
    }

    /**
     * Test user accessing profile.
     */
    @Test
    public void testUserCanAccessOwnProfile() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity prof = helper.getTestFrame("reto",
                AccessibleEntity.class);
        // Check user ISN'T admin (otherwise they'd be able to access anything)
        assertFalse(acl.isAdmin(reto));
        assertTrue(acl.getAccessControl(prof, reto));
    }

    /**
     * Test user accessing other profile.
     */
    @Test
    public void testUserCannotWriteOtherProfile() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity tim = helper.getTestFrame("tim",
                AccessibleEntity.class);
        assertTrue(acl.getAccessControl(tim, reto));
    }

    /**
     * Test user accessing other profile as anonymous.
     */
    @Test
    public void testUserAccessAsAnonymous() {
        Accessor anon = new AnonymousAccessor();
        AccessibleEntity tim = helper.getTestFrame("tim",
                AccessibleEntity.class);
        assertTrue(acl.getAccessControl(tim, anon));
    }
    
    /**
     * Test a member of a group DOES NOT have write access to the group.
     */
    @Test
    public void testUserCannotChangeGroupJustByBeingAMemberOfIt() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity kcl = helper.getTestFrame("kclGroup",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));
    }

    /**
     * Test changing permissions on an item.
     * @throws PermissionDenied 
     */
    @Test
    public void testChangingItemPermissions() throws PermissionDenied {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity kcl = helper.getTestFrame("kclGroup",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));
    }

    /**
     * Test removing permissions.
     * @throws PermissionDenied 
     */
    @Test
    public void testRemovingItemPermissions() throws PermissionDenied {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity kcl = helper.getTestFrame("kclGroup",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now remove the access control...
        acl.removeAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));
    }
}
