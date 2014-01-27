package eu.ehri.project.views;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * User: mikebryant
 */
public class AclViewsTest extends AbstractFixtureTest {
    public void testSetGlobalPermissionMatrix() throws Exception {
        // TODO
    }

    public void testSetAccessors() throws Exception {
        // TODO
    }

    public void testSetItemPermissions() throws Exception {
        // TODO
    }

    public void testRevokePermissionGrant() throws Exception {
        // TODO
    }

    @Test
    public void testValidUserCanAddAccessorToGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        new AclViews(graph).addAccessorToGroup(group, user, validUser);
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotAddAccessorToGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        new AclViews(graph).addAccessorToGroup(group, user, invalidUser);
    }

    @Test
    public void testRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("dans", Group.class);
        new AclViews(graph).removeAccessorFromGroup(group, user, validUser);
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("dans", Group.class);
        new AclViews(graph).removeAccessorFromGroup(group, user, invalidUser);
    }

    @Test
    public void testAddUserToGroupGranteeMembership() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("niod", Group.class);
        Accessor grantee = invalidUser;
        // Grant the user specific permissions to update the group
        new AclManager(graph).grantPermissions(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
                PermissionType.GRANT);
        new AclManager(graph).grantPermissions(grantee, graph.frame(group.asVertex(), PermissionGrantTarget.class),
                PermissionType.UPDATE);
        try {
            // This should still fail, because the user doesn't belong
            // to the group himself...
            new AclViews(graph).addAccessorToGroup(group, user, grantee);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
        }
        // Add the user to the group, so he should then be
        // able to do the adding himself...
        group.addMember(grantee);
        new AclViews(graph).addAccessorToGroup(group, user, grantee);
    }

    @Test
    public void testAddUserToGroupGranteePerms() throws Exception {
        AclManager acl = new AclManager(graph);
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("soma", Group.class);
        Accessor grantee = invalidUser;
        assertFalse(acl.belongsToAdmin(grantee));
        // Grant the user specific permissions to update the group
        //new AclManager(graph).grantPermissions(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
        //        PermissionType.GRANT);
        group.addMember(grantee);
        assertFalse(acl.belongsToAdmin(grantee));
        // Grant UPDATE permissions on the Group
        new AclManager(graph).grantPermissions(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
                PermissionType.GRANT);
        try {
            // This should still fail, because the user does not have UPDATE
            // permissions on the Group
            new AclViews(graph).addAccessorToGroup(group, user, grantee);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
        }
        // Grant UPDATE permissions on the Group
        new AclManager(graph).grantPermissions(grantee, graph.frame(group.asVertex(), PermissionGrantTarget.class),
                PermissionType.UPDATE);
        new AclViews(graph).addAccessorToGroup(group, user, grantee);
    }
}
