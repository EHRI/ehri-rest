package eu.ehri.project.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
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
        Group admin = manager.getFrame("admin", Group.class);
        // check we have some users
        assertTrue(admin.getMembers().iterator().hasNext());
    }

    /**
     * Ensure user 'Reto' can't access collection 'c3'.
     */
    @Test
    public void testAdminRead() {
        Group admin = manager.getFrame("admin", Group.class);
        UserProfile reto = manager.getFrame("reto", UserProfile.class);
        DocumentaryUnit c3 = manager.getFrame("c3", DocumentaryUnit.class);
        assertTrue(acl.getAccessControl(c3, admin));
        assertFalse(acl.getAccessControl(c3, reto));
    }

    /**
     * Test NIOD group has no access to items with admin perms.
     */
    @Test
    public void testNiodGroup() {
        Group niod = manager.getFrame("niod", Group.class);
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        assertFalse(acl.getAccessControl(c1, niod));

        // but we should have read-only access to items with no specified perms.
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        assertTrue(acl.getAccessControl(c4, niod));
    }

    /**
     * Group permissions override user permissions.
     */
    @Test
    public void testUserGroupPermOverride() {
        Accessor tim = manager.getFrame("tim", Accessor.class);
        AccessibleEntity c3 = manager.getFrame("c3", AccessibleEntity.class);
        assertTrue(acl.getAccessControl(c3, tim));
    }

    /**
     * Test user accessing profile.
     */
    @Test
    public void testUserCanAccessOwnProfile() {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity prof = manager.getFrame("reto",
                AccessibleEntity.class);
        // Check user ISN'T admin (otherwise they'd be able to access anything)
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.getAccessControl(prof, reto));
    }

    /**
     * Test user accessing other profile.
     */
    @Test
    public void testUserCannotWriteOtherProfile() {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity tim = manager.getFrame("tim",
                AccessibleEntity.class);
        assertTrue(acl.getAccessControl(tim, reto));
    }

    /**
     * Test user accessing other profile as anonymous.
     */
    @Test
    public void testUserAccessAsAnonymous() {
        AccessibleEntity tim = manager.getFrame("tim",
                AccessibleEntity.class);
        assertTrue(acl.getAccessControl(tim, AnonymousAccessor.getInstance()));
    }

    /**
     * Test a member of a group DOES NOT have write access to the group.
     */
    @Test
    public void testUserCannotChangeGroupJustByBeingAMemberOfIt() {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));
    }

    /**
     * Test changing permissions on an item.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testChangingItemAccessibility() throws PermissionDenied {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));
    }

    /**
     * Test removing permissions.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testRemovingItemAccessibility() throws PermissionDenied {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl",
                AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));

        // Now remove the access control...
        acl.removeAccessControl(kcl, reto);
        assertTrue(acl.getAccessControl(kcl, reto));
    }

    /**
     * Test the global permission matrix.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testGlobalPermissionMatrix() throws PermissionDenied {
        Accessor linda = manager.getFrame("linda", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(linda));

        Map<String, List<String>> cmap = acl.getGlobalPermissions(linda);
        // linda has been granted CREATE access for documentaryUnits.
        assertTrue(cmap.get(EntityTypes.DOCUMENTARY_UNIT).contains(
                PermissionTypes.CREATE));
    }

    @Test
    public void testPermissionSet() throws PermissionDenied {

        String[] perms = { PermissionTypes.CREATE, PermissionTypes.DELETE,
                PermissionTypes.UPDATE, PermissionTypes.GRANT,
                PermissionTypes.ANNOTATE };
        String[] types = { EntityTypes.DOCUMENTARY_UNIT,
                EntityTypes.USER_PROFILE, EntityTypes.AGENT, EntityTypes.GROUP };
        Map<String, List<String>> matrix = new HashMap<String, List<String>>();
        for (String type : types) {
            matrix.put(type, new LinkedList<String>());
            for (String perm : perms)
                matrix.get(type).add(perm);
        }

        Accessor accessor = manager.getFrame("reto", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(accessor));

        // Check initial perms are empty...
        Map<String, List<String>> cmap = acl.getGlobalPermissions(accessor);
        for (String type : types) {
            assertNull(cmap.get(type));
        }

        AclManager acl = new AclManager(graph);
        acl.setGlobalPermissionMatrix(accessor, matrix);

        // Check that everything is still there...
        cmap = acl.getGlobalPermissions(accessor);
        for (String type : types) {
            List<String> ps = cmap.get(type);
            assertNotNull(ps);
            for (String perm : perms) {
                assertTrue(ps.contains(perm));
            }
        }
    }

    /**
     * Test admin perms cannot be set.
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testPermissionSetForAdmin() throws PermissionDenied {

        String[] perms = { PermissionTypes.CREATE };
        String[] types = { EntityTypes.DOCUMENTARY_UNIT };
        Map<String, List<String>> matrix = new HashMap<String, List<String>>();
        for (String type : types) {
            matrix.put(type, new LinkedList<String>());
            for (String perm : perms)
                matrix.get(type).add(perm);
        }

        Accessor accessor = manager.getFrame("admin", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertTrue(acl.belongsToAdmin(accessor));

        // Check initial perms are empty...
        Map<String, List<String>> cmap = acl.getGlobalPermissions(accessor);
        for (String type : types) {
            assertNotNull(cmap.get(type));
        }

        AclManager acl = new AclManager(graph);
        // This should throw PermissionDenied
        acl.setGlobalPermissionMatrix(accessor, matrix);
        fail();
    }
}
