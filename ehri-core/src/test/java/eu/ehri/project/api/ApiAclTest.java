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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import eu.ehri.project.acl.InheritedItemPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ApiAclTest extends AbstractFixtureTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test(expected = PermissionDenied.class)
    public void testSetGlobalPermissionMatrixWithPermissionDenied() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);

        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.COUNTRY, PermissionType.PROMOTE)
                .build();
        // This should barf 'cos linda can't update KCL perms
        api(user).acl().setGlobalPermissionMatrix(group, set);
    }

    @Test
    public void testSetGlobalPermissionMatrix() throws Exception {
        Accessor user = manager.getEntity("mike", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        assertFalse(api(user).aclManager()
                .hasPermission(ContentTypes.COUNTRY, PermissionType.PROMOTE, group));
        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.COUNTRY, PermissionType.PROMOTE)
                .build();
        api(user).acl().setGlobalPermissionMatrix(group, set);
        assertTrue(api(user).aclManager()
                .hasPermission(ContentTypes.COUNTRY, PermissionType.PROMOTE, group));
    }

    @Test
    public void testSetEmptyGlobalPermissionMatrix() throws Exception {
        Accessor actioner = manager.getEntity("mike", Accessor.class);
        Accessor target = manager.getEntity("linda", UserProfile.class);
        GlobalPermissionSet oldGlobalPermissions = api(actioner).aclManager().getGlobalPermissions(target);
        assertTrue(oldGlobalPermissions.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE));
        GlobalPermissionSet emptySet = GlobalPermissionSet.newBuilder().build();
        // This should revoke ALL of Linda's permissions
        InheritedGlobalPermissionSet newInheritedGlobalPermissions =
                api(actioner).acl().setGlobalPermissionMatrix(target, emptySet);
        assertEquals(target.getId(), newInheritedGlobalPermissions.accessorId());
        GlobalPermissionSet newGlobalPermissions = api(actioner).aclManager().getGlobalPermissions(target);
        for (ContentTypes ctype : ContentTypes.values()) {
            for (PermissionType ptype : PermissionType.values()) {
                assertFalse(newGlobalPermissions.has(ctype, ptype));
            }
        }
    }

    @Test
    public void testSetAccessors() throws Exception {
        Accessor actioner = manager.getEntity("mike", Accessor.class);
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        assertFalse(Iterables.contains(group.getAccessors(), user));
        api(actioner).acl().setAccessors(group, Sets.newHashSet(user));
        assertTrue(Iterables.contains(group.getAccessors(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testSetAccessorsWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        assertFalse(Iterables.contains(group.getAccessors(), actioner));
        api(actioner).acl().setAccessors(group, Sets.newHashSet(actioner));
    }

    @Test
    public void testSetItemPermissions() throws Exception {
        Accessor actioner = manager.getEntity("mike", Accessor.class);
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        assertFalse(api(actioner).aclManager().hasPermission(group, PermissionType.DELETE, user));
        InheritedItemPermissionSet inheritedItemPermissionSet = api(actioner).acl()
                .setItemPermissions(group, user, Sets.newHashSet(PermissionType.DELETE));
        assertEquals(user.getId(), inheritedItemPermissionSet.accessorId());
        assertTrue(api(actioner).aclManager().hasPermission(group, PermissionType.DELETE, user));
    }

    @Test(expected = PermissionDenied.class)
    public void testSetItemPermissionsWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        api(actioner).acl()
                .setItemPermissions(group, actioner, Sets.newHashSet(PermissionType.DELETE));
    }

    @Test
    public void testRevokePermissionGrant() throws Exception {
        Accessor actioner = manager.getEntity("mike", Accessor.class);
        Accessor user = manager.getEntity("reto", Accessor.class);
        Repository repo = manager.getEntity("r1", Repository.class);
        PermissionGrant grant = manager.getEntity("retoKclWriteGrant", PermissionGrant.class);
        assertTrue(api(actioner).aclManager().withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
        api(actioner).acl().revokePermissionGrant(grant);
        assertFalse(api(actioner).aclManager().withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
    }

    @Test(expected = PermissionDenied.class)
    public void testRevokePermissionGrantWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getEntity("linda", Accessor.class);
        Accessor user = manager.getEntity("reto", Accessor.class);
        Repository repo = manager.getEntity("r1", Repository.class);
        PermissionGrant grant = manager.getEntity("retoKclWriteGrant", PermissionGrant.class);
        assertTrue(api(actioner).aclManager().withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
        api(actioner).acl().revokePermissionGrant(grant);
    }

    @Test
    public void testValidUserCanAddAccessorToGroup() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        api(validUser).acl().addAccessorToGroup(group, user);
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotAddAccessorToGroup() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("kcl", Group.class);
        api(invalidUser).acl().addAccessorToGroup(group, user);
    }

    @Test
    public void testRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("dans", Group.class);
        assertTrue(Lists.newArrayList(group.getMembers()).contains(user));
        api(validUser).acl().removeAccessorFromGroup(group, user);
        assertFalse(Lists.newArrayList(group.getMembers()).contains(user));
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("dans", Group.class);
        api(invalidUser).acl().removeAccessorFromGroup(group, user);
    }

    @Test
    public void testAddUserToGroupGranteeMembership() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("niod", Group.class);
        Accessor grantee = invalidUser;
        // Grant the user specific permissions to update the group
        api(grantee).aclManager().grantPermission(
                user.as(PermissionGrantTarget.class), PermissionType.GRANT, grantee);
        api(grantee).aclManager().grantPermission(
                group.as(PermissionGrantTarget.class), PermissionType.UPDATE, grantee);
        try {
            // This should still fail, because the user doesn't belong
            // to the group himself...
            api(grantee).acl().addAccessorToGroup(group, user);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
            // expected...
        }
        // Add the user to the group, so he should then be
        // able to do the adding himself...
        group.addMember(grantee);
        api(grantee).acl().addAccessorToGroup(group, user);
    }

    @Test
    public void testAddUserToGroupGranteePerms() throws Exception {
        Accessor user = manager.getEntity("linda", Accessor.class);
        Group group = manager.getEntity("soma", Group.class);
        Accessor grantee = invalidUser;
        assertFalse(AclManager.belongsToAdmin(grantee));
        // Grant the user specific permissions to update the group
        group.addMember(grantee);
        assertFalse(AclManager.belongsToAdmin(grantee));
        // Grant UPDATE permissions on the Group
        api(grantee).aclManager().grantPermission(graph.frame(user.asVertex(),
                PermissionGrantTarget.class), PermissionType.GRANT, grantee);
        try {
            // This should still fail, because the user does not have UPDATE
            // permissions on the Group
            api(grantee).acl().addAccessorToGroup(group, user);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
            // expected...
        }
        // Grant UPDATE permissions on the Group
        api(grantee).aclManager().grantPermission(
                group.as(PermissionGrantTarget.class), PermissionType.UPDATE, grantee);
        api(grantee).acl().addAccessorToGroup(group, user);
    }
}
