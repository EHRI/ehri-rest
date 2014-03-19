package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.test.TestData;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.acl.ContentTypes.COUNTRY;
import static eu.ehri.project.acl.ContentTypes.DOCUMENTARY_UNIT;
import static eu.ehri.project.acl.ContentTypes.REPOSITORY;
import static eu.ehri.project.acl.PermissionType.*;
import static org.junit.Assert.*;

/**
 * User: michaelb
 */
public class AclManagerTest extends GraphTestBase {

    private FixtureLoader loader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loader = FixtureLoaderFactory.getInstance(graph, false); // Initialize separately
        new GraphInitializer(graph).initialize();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIsAdmin() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        assertTrue(new AclManager(graph).belongsToAdmin(admin));
    }

    @Test
    public void testBelongsToAdmin() throws Exception {
        loader.loadTestData();
        UserProfile mike = manager.getFrame("mike", UserProfile.class);
        assertTrue(new AclManager(graph).belongsToAdmin(mike));

        // Test a user not directly in admin, but belonging to a group that is,
        // is also an admin user.
        UserProfile tim = manager.getFrame("tim", UserProfile.class);
        assertTrue(new AclManager(graph).belongsToAdmin(tim));
    }

    @Test
    public void testIsAnonymous() throws Exception {
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        Accessor anon = AnonymousAccessor.getInstance();
        AclManager acl = new AclManager(graph);
        assertFalse(acl.belongsToAdmin(anon));
        assertTrue(acl.isAnonymous(anon));
        assertFalse(acl.isAnonymous(admin));
    }

    @Test
    public void testGetAccessControl() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getFrame("mike", UserProfile.class);
        UserProfile user2 = manager.getFrame("reto", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(acl.canAccess(c1, user1));
        assertFalse(acl.canAccess(c1, user2));
    }

    @Test
    public void testRemoveAccessControl() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getFrame("mike", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(Iterables.contains(c1.getAccessors(), user1));
        acl.removeAccessControl(c1, user1);
        assertFalse(Iterables.contains(c1.getAccessors(), user1));
    }

    @Test
    public void testSetAccessors() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getFrame("mike", UserProfile.class);
        UserProfile user2 = manager.getFrame("reto", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(acl.canAccess(c1, user1));
        assertFalse(acl.canAccess(c1, user2));
        acl.setAccessors(c1, Lists.<Accessor>newArrayList(user1, user2));
        assertTrue(acl.canAccess(c1, user1));
        assertTrue(acl.canAccess(c1, user2));
    }

    @Test
    public void testGetInheritedEntityPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        UserProfile user = manager.getFrame("reto", UserProfile.class);

        // ? Because we can create docs within this scope (r1), we
        // can also create CHILD docs of docs within that scope.
        // Should this behaviour be allowed? For the moment it
        // is...
        List<Map<String, List<PermissionType>>> permissions
                = acl.getInheritedEntityPermissions(user, c4);
        assertTrue(hasPermissionIn(permissions, user, CREATE));
        assertFalse(hasPermissionIn(permissions, user, UPDATE));
        assertFalse(hasPermissionIn(permissions, user, DELETE));
        assertFalse(hasPermissionIn(permissions, user, OWNER));
    }

    @Test
    public void testSetEntityPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        UserProfile user = manager.getFrame("reto", UserProfile.class);

        // ? Because we can create docs within this scope (r1), we
        // can also create CHILD docs of docs within that scope.
        // Should this behaviour be allowed? For the moment it
        // is...
        List<Map<String, List<PermissionType>>> permissions
                = acl.getInheritedEntityPermissions(user, c4);
        assertTrue(hasPermissionIn(permissions, user, CREATE));
        assertFalse(hasPermissionIn(permissions, user, UPDATE));
        assertFalse(hasPermissionIn(permissions, user, DELETE));
        assertFalse(hasPermissionIn(permissions, user, OWNER));

        acl.setEntityPermissions(user, c4, Sets.newHashSet(DELETE, UPDATE));
        List<Map<String, List<PermissionType>>> permissions2
                = Lists.newArrayList(acl.getInheritedEntityPermissions(user, c4));
        assertTrue(hasPermissionIn(permissions2, user, CREATE));
        assertTrue(hasPermissionIn(permissions2, user, UPDATE));
        assertTrue(hasPermissionIn(permissions2, user, DELETE));
        assertFalse(hasPermissionIn(permissions2, user, OWNER));
    }

    @Test
    public void testSetPermissionMatrix() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getFrame("reto", UserProfile.class);

        GlobalPermissionSet permissions = acl.getGlobalPermissions(user);
        assertFalse(permissions.has(COUNTRY, CREATE));
        acl.setPermissionMatrix(user, permissions.withPermission(COUNTRY, CREATE));
        assertTrue(acl.getGlobalPermissions(user).has(COUNTRY, CREATE));
    }

    @Test
    public void testGrantPermission() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getFrame("reto", UserProfile.class);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);

        assertFalse(acl.hasPermission(c4, OWNER, user));
        PermissionGrant grant = acl.grantPermission(user, c4, OWNER);
        assertTrue(acl.hasPermission(c4, OWNER, user));

        // Try granting the same permission twice and ensure the
        // returned grant is the same instance as before...
        PermissionGrant grant2 = acl.grantPermission(user, c4, OWNER);
        assertEquals(grant, grant2);
    }

    @Test
    public void testContentTypeFilterFunction() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);

        Vertex c1 = manager.getVertex("c1");
        Vertex r1 = manager.getVertex("r1");
        // cd1 is not a content type
        Vertex cd1 = manager.getVertex("cd1");
        List<Vertex> frames = Lists.newArrayList(c1, r1, cd1);

        GremlinPipeline<Vertex, Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(frames);
        List<Vertex> filtered = pipeline
                .filter(acl.getContentTypeFilterFunction()).toList();
        assertEquals(2L, filtered.size());
        assertFalse(filtered.contains(cd1));
    }

    @Test
    public void testGetAclFilterFunction() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);

        UserProfile user = manager.getFrame("reto", UserProfile.class);

        Annotation ann3v = manager.getFrame("ann3", Annotation.class); // hidden from user
        Annotation ann4v = manager.getFrame("ann4", Annotation.class); // promoted, thus visible
        assertFalse(acl.canAccess(ann3v, user));
        assertTrue(acl.canAccess(ann4v, user));

        PipeFunction<Vertex,Boolean> aclFilter = acl.getAclFilterFunction(user);
        List<Vertex> filtered = new GremlinPipeline<Vertex,Vertex>(
                Lists.newArrayList(ann3v.asVertex(), ann4v.asVertex())).filter(aclFilter).toList();
        assertEquals(1L, filtered.size());
        assertFalse(filtered.contains(ann3v.asVertex()));
    }

    @Test
    public void testRevokePermission() throws Exception {

        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getFrame("reto", UserProfile.class);
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);

        assertFalse(acl.hasPermission(c4, OWNER, user));
        acl.grantPermission(user, c4, OWNER);
        assertTrue(acl.hasPermission(c4, OWNER, user));
        acl.revokePermission(user, c4, OWNER);
        assertFalse(acl.hasPermission(c4, OWNER, user));
    }

    @Test
    public void testRevokePermissionGrant() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getFrame("reto", UserProfile.class);
        Repository r1 = manager.getFrame("r1", Repository.class);
        PermissionGrant grant
                = manager.getFrame("retoKclWriteGrant", PermissionGrant.class);
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, user));
        assertTrue(acl.withScope(r1).hasPermission(DOCUMENTARY_UNIT, CREATE, user));
        acl.revokePermissionGrant(grant);
        assertFalse(acl.withScope(r1).hasPermission(DOCUMENTARY_UNIT, CREATE, user));
    }

    @Test
    public void testGetAdminPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        GlobalPermissionSet permissionSet = acl.getGlobalPermissions(admin);
        // Admin should have ALL the permissions!
        for (ContentTypes contentType : ContentTypes.values()) {
            for (PermissionType permissionType : PermissionType.values()) {
                assertTrue(permissionSet.has(contentType, permissionType));
            }
        }
    }

    @Test
    public void testGetInheritedGlobalPermissions() throws Exception {
        loader.loadTestData("permissions.yaml");
        Group group1 = manager.getFrame("group1", Group.class);
        UserProfile user1 = manager.getFrame("user1", UserProfile.class);

        AclManager acl = new AclManager(graph);
        List<Map<String, GlobalPermissionSet>> getPerms
                = acl.getInheritedGlobalPermissions(user1);
        // It should contain two elements - the user, and his group
        assertEquals(2, getPerms.size());
        assertEquals(GlobalPermissionSet.empty(), getPerms.get(0).get(user1.getId()));
        GlobalPermissionSet groupPerms = acl.getGlobalPermissions(group1);
        assertEquals(groupPerms, getPerms.get(1).get(group1.getId()));
    }

    @Test
    public void testGetGlobalPermissions() throws Exception {
        loader.loadTestData("permissions.yaml");
        Group group1 = manager.getFrame("group1", Group.class);
        UserProfile user1 = manager.getFrame("user1", UserProfile.class);

        // Group1 should have create/update/delete for documentaryUnits
        GlobalPermissionSet hasPerms = GlobalPermissionSet.newBuilder()
            .set(DOCUMENTARY_UNIT, CREATE, UPDATE, DELETE).build();
        GlobalPermissionSet getPerms = new AclManager(graph).getGlobalPermissions(group1);

        // Bit of a hassle comparing the data structures..
        assertEquals(hasPerms, getPerms);

        // User1 should have an empty set of global permissions
        assertEquals(GlobalPermissionSet.empty(), new AclManager(graph)
                .getGlobalPermissions(user1));
    }

    @Test
    public void testCountryScopeScenario() throws Exception {
        loader.loadTestData("country-permissions.yaml");

        Country gb = manager.getFrame("gb", Country.class);
        Country nl = manager.getFrame("nl", Country.class);
        UserProfile gbuser = manager.getFrame("gbuser", UserProfile.class);
        UserProfile nluser = manager.getFrame("nluser", UserProfile.class);

        AclManager acl = new AclManager(graph);

        assertTrue(acl.withScope(gb).hasPermission(REPOSITORY, CREATE, gbuser));
        assertFalse(acl.withScope(nl).hasPermission(REPOSITORY, CREATE, gbuser));

        // Create a repository
        // New create some stuff
        LoggingCrudViews<Repository> repoViews
                = new LoggingCrudViews<Repository>(graph, Repository.class);

        Repository gbrepo = repoViews.setScope(gb).create(
                Bundle.fromData(TestData.getTestAgentBundle()), gbuser);
        Repository nlrepo = repoViews.setScope(nl).create(
                Bundle.fromData(TestData.getTestAgentBundle()), nluser);

        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, CREATE, gbuser));
        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, UPDATE, gbuser));
        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, DELETE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, CREATE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, UPDATE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, DELETE, gbuser));
    }

    @Test
    public void testCreateOwnerScenario() throws Exception {
        loader.loadTestData("archivist-permissions.yaml");
        UserProfile headUser = manager.getFrame("hauser", UserProfile.class);
        UserProfile user1 = manager.getFrame("auser1", UserProfile.class);
        UserProfile user2 = manager.getFrame("auser2", UserProfile.class);
        Group headArchivists = manager.getFrame("head-archivists", Group.class);
        Group archivists = manager.getFrame("archivists", Group.class);
        Repository repo = manager.getFrame("repo", Repository.class);

        AclManager acl = new AclManager(graph, repo);
        // The grants in the permissions should give these particular
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, headArchivists));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, headArchivists));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, headArchivists));

        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, archivists));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, archivists));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, archivists));

        // Check these grants are correctly inherited by the users.
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, headUser));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, headUser));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, headUser));

        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, user1));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, user1));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, user1));

        // New create some stuff
        LoggingCrudViews<DocumentaryUnit> views
                = new LoggingCrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class, repo);

        DocumentaryUnit headdoc1 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "head-doc"), headUser);
        DocumentaryUnit userdoc1 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "user-doc-1"), user1);
        DocumentaryUnit userdoc2 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "user-doc-2"), user2);

        // Ensure Head Archivist can update/delete user1's doc
        assertTrue(acl.hasPermission(userdoc1, UPDATE, headArchivists));
        assertTrue(acl.hasPermission(userdoc1, DELETE, headArchivists));
        // But doesn't own it
        assertFalse(acl.hasPermission(userdoc1, OWNER, headArchivists));

        // Check the calculated permission sets.
        assertTrue(hasPermissionIn(
                acl.getInheritedEntityPermissions(user1, userdoc1), user1, OWNER));
        assertFalse(hasPermissionIn(
                acl.getInheritedEntityPermissions(user1, userdoc2), user1, OWNER));
        assertTrue(hasPermissionIn(
                acl.getInheritedEntityPermissions(user2, userdoc2), user2, OWNER));
        assertFalse(hasPermissionIn(
                acl.getInheritedEntityPermissions(user2, userdoc1), user2, OWNER));

        // Ensure user1 can update/delete his own doc
        assertTrue(acl.hasPermission(userdoc1, OWNER, user1));
        assertTrue(acl.hasPermission(userdoc1, UPDATE, user1));
        assertTrue(acl.hasPermission(userdoc1, DELETE, user1));

        // Ensure neither user1 or user2 can update/delete head's docs
        assertFalse(acl.hasPermission(headdoc1, OWNER, user1));
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user1));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user1));
        assertFalse(acl.hasPermission(headdoc1, OWNER, user2));
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user2));

        // Ensure neither user1 or user2 can update/delete each other's docs
        assertFalse(acl.hasPermission(userdoc1, OWNER, user2));
        assertFalse(acl.hasPermission(userdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(userdoc1, DELETE, user2));
        assertFalse(acl.hasPermission(userdoc2, OWNER, user1));
        assertFalse(acl.hasPermission(userdoc2, UPDATE, user1));
        assertFalse(acl.hasPermission(userdoc2, DELETE, user1));
    }

    private boolean hasPermissionIn(List<Map<String, List<PermissionType>>> set, Accessor user, PermissionType perm) {
        for (Map<String, List<PermissionType>> grant : set) {
            if (grant.containsKey(user.getId())) {
                if (grant.get(user.getId()).contains(perm)) {
                    return true;
                }
            }
        }
        return false;
    }
}
