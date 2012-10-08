package eu.ehri.project.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
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
import eu.ehri.project.relationships.Access;

public class AclTest extends ModelTestBase {

    private AclManager acl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() {
        super.setUp();
        acl = new AclManager(graph);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
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
    public void testAdminReadWrite() {
        Group admin = helper.getTestFrame("adminGroup", Group.class);
        UserProfile reto = helper.getTestFrame("reto", UserProfile.class);
        DocumentaryUnit c3 = helper.getTestFrame("c3", DocumentaryUnit.class);
        Access adminAccess = acl.getAccessControl(c3, admin);
        Access retoAccess = acl.getAccessControl(c3, reto);

        assertTrue(adminAccess.getRead());
        assertTrue(adminAccess.getWrite());
        assertFalse(retoAccess.getRead());
        assertFalse(retoAccess.getWrite());
    }

    /**
     * Test NIOD group has no access to items with admin perms.
     */
    @Test
    public void testNiodGroup() {
        Group niod = helper.getTestFrame("niodGroup", Group.class);
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        Access access1 = acl.getAccessControl(c1, niod);
        assertFalse(access1.getRead());
        assertFalse(access1.getWrite());

        // but we should have read-only access to items with no specified perms.
        DocumentaryUnit c4 = helper.getTestFrame("c4", DocumentaryUnit.class);
        Access access2 = acl.getAccessControl(c4, niod);
        assertTrue(access2.getRead());
        assertFalse(access2.getWrite());
    }

    /**
     * Group permissions override user permissions.
     */
    @Test
    public void testUserGroupPermOverride() {
        Accessor tim = helper.getTestFrame("tim", Accessor.class);
        AccessibleEntity c3 = helper.getTestFrame("c3", AccessibleEntity.class);
        Access access = acl.getAccessControl(c3, tim);
        assertTrue(access.getRead());
        assertTrue(access.getWrite());
    }

    /**
     * Test user accessing profile.
     */
    @Test
    public void testUserCanAccessOwnProfile() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity prof = helper.getTestFrame("reto",
                AccessibleEntity.class);
        Access access = acl.getAccessControl(prof, reto);
        // Check user ISN'T admin (otherwise they'd be able to access anything)
        assertFalse(acl.isAdmin(reto));
        assertTrue(access.getRead());
        assertTrue(access.getWrite());
    }

    /**
     * Test user accessing other profile.
     */
    @Test
    public void testUserCannotWriteOtherProfile() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity tim = helper.getTestFrame("tim",
                AccessibleEntity.class);
        Access access = acl.getAccessControl(tim, reto);
        assertTrue(access.getRead());
        assertFalse(access.getWrite());
    }

    /**
     * Test user accessing other profile as anonymous.
     */
    @Test
    public void testUserAccessAsAnonymous() {
        Accessor anon = new AnonymousAccessor();
        AccessibleEntity tim = helper.getTestFrame("tim",
                AccessibleEntity.class);
        Access access = acl.getAccessControl(tim, anon);
        assertTrue(access.getRead());
        assertFalse(access.getWrite());
    }
    
    /**
     * Test a member of a group DOES NOT have write access to the group.
     */
    @Test
    public void testUserCannotChangeGroupJustByBeingAMemberOfIt() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity kcl = helper.getTestFrame("kclGroup",
                AccessibleEntity.class);
        Access access = acl.getAccessControl(kcl, reto);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(access.getRead());
        assertFalse(access.getWrite());
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
        Access access = acl.getAccessControl(kcl, reto);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(access.getRead());
        assertFalse(access.getWrite());

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto, true, true);
        access = acl.getAccessControl(kcl, reto);
        assertTrue(access != null);
        assertTrue(access.getWrite());
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
        Access access = acl.getAccessControl(kcl, reto);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.isAdmin(reto));
        assertTrue(access.getRead());
        assertFalse(access.getWrite());

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto, true, true);
        access = acl.getAccessControl(kcl, reto);
        assertTrue(access != null);
        assertTrue(access.getWrite());

        // Now remove the access control...
        acl.removeAccessControl(kcl, reto);
        access = acl.getAccessControl(kcl, reto);
        assertTrue(access != null);
        assertFalse(access.getWrite());
    }
}
