package eu.ehri.project.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships.Access;

public class AclTest extends ModelTestBase {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() {
        super.setUp();
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
        Access adminAccess = AclManager.getAccessControl(admin, c3);
        Access retoAccess = AclManager.getAccessControl(reto, c3);

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
        Access access1 = AclManager.getAccessControl(niod, c1);
        assertFalse(access1.getRead());
        assertFalse(access1.getWrite());

        // but we should have read-only access to items with no specified perms.
        DocumentaryUnit c4 = helper.getTestFrame("c4", DocumentaryUnit.class);
        Access access2 = AclManager.getAccessControl(niod, c4);
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
        Access access = AclManager.getAccessControl(tim, c3);
        assertTrue(access.getRead());
        assertTrue(access.getWrite());
    }
    
    @Test
    public void testUserCanAccessOwnProfile() {
        Accessor tim = helper.getTestFrame("tim", Accessor.class);
        AccessibleEntity prof = helper.getTestFrame("tim", AccessibleEntity.class);
        Access access = AclManager.getAccessControl(tim, prof);
        assertTrue(access.getRead());
        assertTrue(access.getWrite());        
    }
    
    @Test
    public void testUserCannotWriteOtherProfile() {
        Accessor reto = helper.getTestFrame("reto", Accessor.class);
        AccessibleEntity tim = helper.getTestFrame("tim", AccessibleEntity.class);
        Access access = AclManager.getAccessControl(reto, tim);
        assertTrue(access.getRead());
        assertFalse(access.getWrite());        
    }
}
