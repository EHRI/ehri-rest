package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: mikebryant
 */
public class AclViewsTest extends AbstractFixtureTest {

    private AclViews aclViews;
    private AclManager aclManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        aclViews = new AclViews(graph);
        aclManager = new AclManager(graph);
    }

    @Test(expected = PermissionDenied.class)
    public void testSetGlobalPermissionMatrixWithPermissionDenied() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);

        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.COUNTRY, PermissionType.PROMOTE)
                .build();
        // This should barf 'cos linda can't update KCL perms
        aclViews.setGlobalPermissionMatrix(group, set, user);
    }

    @Test
    public void testSetGlobalPermissionMatrix() throws Exception {
        Accessor user = manager.getFrame("mike", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        assertFalse(aclManager
                .hasPermission(ContentTypes.COUNTRY, PermissionType.PROMOTE, group));
        GlobalPermissionSet set = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.COUNTRY, PermissionType.PROMOTE)
                .build();
        aclViews.setGlobalPermissionMatrix(group, set, user);
        assertTrue(aclManager
                .hasPermission(ContentTypes.COUNTRY, PermissionType.PROMOTE, group));
    }

    @Test
    public void testSetAccessors() throws Exception {
        Accessor actioner = manager.getFrame("mike", Accessor.class);
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        assertFalse(Iterables.contains(group.getAccessors(), user));
        aclViews.setAccessors(group, Sets.newHashSet(user), actioner);
        assertTrue(Iterables.contains(group.getAccessors(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testSetAccessorsWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        assertFalse(Iterables.contains(group.getAccessors(), actioner));
        aclViews.setAccessors(group, Sets.newHashSet(actioner), actioner);
    }

    @Test
    public void testSetItemPermissions() throws Exception {
        Accessor actioner = manager.getFrame("mike", Accessor.class);
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        assertFalse(aclManager.hasPermission(group, PermissionType.DELETE, user));
        aclViews
                .setItemPermissions(group, user, Sets.newHashSet(PermissionType.DELETE), actioner);
        assertTrue(aclManager.hasPermission(group, PermissionType.DELETE, user));
    }

    @Test(expected = PermissionDenied.class)
    public void testSetItemPermissionsWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        aclViews
                .setItemPermissions(group, actioner, Sets.newHashSet(PermissionType.DELETE), actioner);
    }

    @Test
    public void testRevokePermissionGrant() throws Exception {
        Accessor actioner = manager.getFrame("mike", Accessor.class);
        Accessor user = manager.getFrame("reto", Accessor.class);
        Repository repo = manager.getFrame("r1", Repository.class);
        PermissionGrant grant = manager.getFrame("retoKclWriteGrant", PermissionGrant.class);
        assertTrue(aclManager.withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
        aclViews.revokePermissionGrant(grant, actioner);
        assertFalse(aclManager.withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
    }

    @Test(expected = PermissionDenied.class)
    public void testRevokePermissionGrantWithPermissionDenied() throws Exception {
        Accessor actioner = manager.getFrame("linda", Accessor.class);
        Accessor user = manager.getFrame("reto", Accessor.class);
        Repository repo = manager.getFrame("r1", Repository.class);
        PermissionGrant grant = manager.getFrame("retoKclWriteGrant", PermissionGrant.class);
        assertTrue(aclManager.withScope(repo)
                .hasPermission(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE, user));
        aclViews.revokePermissionGrant(grant, actioner);
    }

    @Test
    public void testValidUserCanAddAccessorToGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        aclViews.addAccessorToGroup(group, user, validUser);
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotAddAccessorToGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("kcl", Group.class);
        aclViews.addAccessorToGroup(group, user, invalidUser);
    }

    @Test
    public void testRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("dans", Group.class);
        aclViews.removeAccessorFromGroup(group, user, validUser);
    }

    @Test(expected = PermissionDenied.class)
    public void testInvalidUserCannotRemoveAccessorFromGroup() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("dans", Group.class);
        aclViews.removeAccessorFromGroup(group, user, invalidUser);
    }

    @Test
    public void testAddUserToGroupGranteeMembership() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("niod", Group.class);
        Accessor grantee = invalidUser;
        // Grant the user specific permissions to update the group
        aclManager.grantPermission(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
                PermissionType.GRANT);
        aclManager.grantPermission(grantee, graph.frame(group.asVertex(), PermissionGrantTarget.class),
                PermissionType.UPDATE);
        try {
            // This should still fail, because the user doesn't belong
            // to the group himself...
            aclViews.addAccessorToGroup(group, user, grantee);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
        }
        // Add the user to the group, so he should then be
        // able to do the adding himself...
        group.addMember(grantee);
        aclViews.addAccessorToGroup(group, user, grantee);
    }

    @Test
    public void testAddUserToGroupGranteePerms() throws Exception {
        Accessor user = manager.getFrame("linda", Accessor.class);
        Group group = manager.getFrame("soma", Group.class);
        Accessor grantee = invalidUser;
        assertFalse(aclManager.belongsToAdmin(grantee));
        // Grant the user specific permissions to update the group
        //new AclManager(graph).grantPermission(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
        //        PermissionType.GRANT);
        group.addMember(grantee);
        assertFalse(aclManager.belongsToAdmin(grantee));
        // Grant UPDATE permissions on the Group
        aclManager.grantPermission(grantee, graph.frame(user.asVertex(), PermissionGrantTarget.class),
                PermissionType.GRANT);
        try {
            // This should still fail, because the user does not have UPDATE
            // permissions on the Group
            aclViews.addAccessorToGroup(group, user, grantee);
            fail("User should NOT have had grant permissions!");
        } catch (PermissionDenied e) {
        }
        // Grant UPDATE permissions on the Group
        aclManager.grantPermission(grantee, graph.frame(group.asVertex(), PermissionGrantTarget.class),
                PermissionType.UPDATE);
        aclViews.addAccessorToGroup(group, user, grantee);
    }
}
