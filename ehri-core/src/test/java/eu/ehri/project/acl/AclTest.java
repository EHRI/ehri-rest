/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.acl;

import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AclTest extends ModelTestBase {

    private AclManager acl;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        acl = new AclManager(graph);
    }

    @Test
    public void testTheFixturesLoaded() {
        assertTrue(graph.getVertices().iterator().hasNext());
    }

    @Test
    public void testTheAdminGroup() throws ItemNotFound {
        Group admin = manager.getFrame("admin", Group.class);
        // check we have some users
        assertTrue(admin.getMembers().iterator().hasNext());
    }

    /**
     * Ensure user 'Reto' can't access collection 'c3'.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testAdminRead() throws ItemNotFound {
        Group admin = manager.getFrame("admin", Group.class);
        UserProfile reto = manager.getFrame("reto", UserProfile.class);
        DocumentaryUnit c3 = manager.getFrame("c3", DocumentaryUnit.class);
        assertTrue(acl.canAccess(c3, admin));
        assertFalse(acl.canAccess(c3, reto));
    }

    /**
     * Test KCL group has no access to items with admin perms.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testNiodGroup() throws ItemNotFound {
        Group kcl = manager.getFrame("kcl", Group.class);
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        assertFalse(acl.canAccess(c1, kcl));

        // but we should have read-only access to items with no specified perms.
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        assertTrue(acl.canAccess(c4, kcl));
    }

    /**
     * Group permissions override user permissions.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testUserGroupPermOverride() throws ItemNotFound {
        Accessor tim = manager.getFrame("tim", Accessor.class);
        AccessibleEntity c3 = manager.getFrame("c3", AccessibleEntity.class);
        assertTrue(acl.canAccess(c3, tim));
    }

    /**
     * Test user accessing profile.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testUserCanAccessOwnProfile() throws ItemNotFound {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity prof = manager
                .getFrame("reto", AccessibleEntity.class);
        // Check user ISN'T admin (otherwise they'd be able to access anything)
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.canAccess(prof, reto));
    }

    /**
     * Test scoped permissions. In the fixtures, 'reto' has permissions
     * to create docs within repository r1, but not update or delete them.
     *
     */
    @Test
    public void testSetUserPermissionsWithScope() throws Exception {
        UserProfile user = manager.getFrame("reto", UserProfile.class);
        Repository scope = manager.getFrame("r1", Repository.class);
        assertFalse(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.UPDATE, user));
        assertFalse(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE, user));
        assertFalse(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.ANNOTATE, user));

        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.DOCUMENTARY_UNIT,
                        PermissionType.UPDATE, PermissionType.DELETE).build();
        acl.withScope(scope).setPermissionMatrix(user, set);
        assertTrue(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.UPDATE, user));
        assertTrue(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE, user));
        // This should still be false, since we didn't change it.
        assertFalse(acl.withScope(scope)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.ANNOTATE, user));
    }



    /**
     * Test user accessing other profile.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testUserCannotWriteOtherProfile() throws ItemNotFound {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity tim = manager.getFrame("tim", AccessibleEntity.class);
        assertTrue(acl.canAccess(tim, reto));
    }

    /**
     * Test user accessing other profile as anonymous.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testUserAccessAsAnonymous() throws ItemNotFound {
        AccessibleEntity tim = manager.getFrame("tim", AccessibleEntity.class);
        assertTrue(acl.canAccess(tim, AnonymousAccessor.getInstance()));
    }

    /**
     * Test a member of a group DOES NOT have write access to the group.
     * 
     * @throws ItemNotFound
     */
    @Test
    public void testUserCannotChangeGroupJustByBeingAMemberOfIt()
            throws ItemNotFound {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl", AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.canAccess(kcl, reto));
    }

    /**
     * Test changing permissions on an item.
     * 
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @Test
    public void testChangingItemAccessibility() throws PermissionDenied,
            ItemNotFound {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl", AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.canAccess(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessors(kcl, Lists.newArrayList(reto));
        assertTrue(acl.canAccess(kcl, reto));
    }

    /**
     * Test removing permissions.
     * 
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @Test
    public void testRemovingItemAccessibility() throws PermissionDenied,
            ItemNotFound {
        Accessor reto = manager.getFrame("reto", Accessor.class);
        AccessibleEntity kcl = manager.getFrame("kcl", AccessibleEntity.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(reto));
        assertTrue(acl.canAccess(kcl, reto));

        // Now set the access control on KCL so Reto can write to it...
        acl.setAccessors(kcl, Lists.newArrayList(reto));
        assertTrue(acl.canAccess(kcl, reto));

        // Now remove the access control...
        acl.removeAccessControl(kcl, reto);
        assertTrue(acl.canAccess(kcl, reto));
    }

    /**
     * Test the global permission matrix.
     * 
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @Test
    public void testGlobalPermissionMatrix() throws PermissionDenied,
            ItemNotFound {
        Accessor linda = manager.getFrame("linda", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(linda));

        GlobalPermissionSet cmap = acl.getGlobalPermissions(linda);
        // linda has been granted CREATE access for DocumentaryUnits.
        assertTrue(cmap.get(ContentTypes.DOCUMENTARY_UNIT).contains(
                PermissionType.CREATE));
    }

    @Test
    public void testPermissionSet() throws PermissionDenied, ItemNotFound {

        PermissionType[] perms = { PermissionType.CREATE,
                PermissionType.DELETE, PermissionType.UPDATE,
                PermissionType.GRANT, PermissionType.ANNOTATE };
        ContentTypes[] types = { ContentTypes.DOCUMENTARY_UNIT,
                ContentTypes.USER_PROFILE, ContentTypes.REPOSITORY,
                ContentTypes.GROUP };
        GlobalPermissionSet.Builder builder = GlobalPermissionSet.newBuilder();
        for (ContentTypes type : types) {
            builder.set(type, perms);
        }
        GlobalPermissionSet set = builder.build();

        Accessor accessor = manager.getFrame("reto", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertFalse(acl.belongsToAdmin(accessor));

        // Check initial perms are empty...
        GlobalPermissionSet cmap = acl.getGlobalPermissions(accessor);
        for (ContentTypes type : types) {
            assertTrue(cmap.get(type).isEmpty());
        }

        AclManager acl = new AclManager(graph);
        acl.setPermissionMatrix(accessor, set);

        // Check that everything is still there...
        cmap = acl.getGlobalPermissions(accessor);
        for (ContentTypes type : types) {
            Collection<PermissionType> ps = cmap.get(type);
            assertNotNull(ps);
            for (PermissionType perm : perms) {
                assertTrue(ps.contains(perm));
            }
        }
    }

    /**
     * Test admin perms cannot be set.
     * 
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @Test(expected = PermissionDenied.class)
    public void testPermissionSetForAdmin() throws PermissionDenied,
            ItemNotFound {

        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE).build();

        Accessor accessor = manager.getFrame("admin", Accessor.class);
        // Admin can change anything, so ensure the user ISN'T a member of admin
        assertTrue(acl.belongsToAdmin(accessor));

        // Check initial perms are empty...
        GlobalPermissionSet cmap = acl.getGlobalPermissions(accessor);
        assertFalse(cmap.get(ContentTypes.DOCUMENTARY_UNIT).isEmpty());

        AclManager acl = new AclManager(graph);
        // This should throw PermissionDenied
        acl.setPermissionMatrix(accessor, set);
        fail();
    }
}
