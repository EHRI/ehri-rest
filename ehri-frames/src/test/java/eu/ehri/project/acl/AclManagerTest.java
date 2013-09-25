package eu.ehri.project.acl;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.test.TestData;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.acl.ContentTypes.DOCUMENTARY_UNIT;
import static eu.ehri.project.acl.ContentTypes.REPOSITORY;
import static eu.ehri.project.acl.PermissionType.*;
import static org.junit.Assert.*;

/**
 * User: michaelb
 */
public class AclManagerTest extends GraphTestBase {

    private GraphCleaner cleaner;
    private FixtureLoader loader;
    private GraphInitializer initializer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        initializer = new GraphInitializer(graph);
        loader = FixtureLoaderFactory.getInstance(graph, false); // Initialize separately
        cleaner = new GraphCleaner(graph);
        initializer.initialize();
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

    }

    @Test
    public void testRemoveAccessControl() throws Exception {

    }

    @Test
    public void testSetAccessors() throws Exception {

    }

    @Test
    public void testGetPermissionGrants() throws Exception {

    }

    @Test
    public void testGetInheritedEntityPermissions() throws Exception {

    }

    @Test
    public void testSetEntityPermissions() throws Exception {

    }

    @Test
    public void testGetInheritedGlobalPermissions() throws Exception {
        loader.loadTestData("permissions.yaml");
        Group group1 = manager.getFrame("group1", Group.class);
        Group group2 = manager.getFrame("group2", Group.class);
        UserProfile user1 = manager.getFrame("user1", UserProfile.class);
        UserProfile user2 = manager.getFrame("user2", UserProfile.class);

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
        GlobalPermissionSet hasPerms = GlobalPermissionSet.empty();
        hasPerms.setContentType(DOCUMENTARY_UNIT, CREATE, UPDATE, DELETE);
        GlobalPermissionSet getPerms = new AclManager(graph).getGlobalPermissions(group1);

        // Bit of a hassle comparing the data structures..
        assertEquals(hasPerms, getPerms);

        // User1 should have an empty set of global permissions
        assertEquals(GlobalPermissionSet.empty(), new AclManager(graph)
                .getGlobalPermissions(user1));
    }

    @Test
    public void testSetPermissionMatrix() throws Exception {

    }

    @Test
    public void testGrantPermissions() throws Exception {

    }

    @Test
    public void testRevokePermissions() throws Exception {

    }

    @Test
    public void testRevokePermissionGrant() throws Exception {

    }

    @Test
    public void testGetAclFilterFunction() throws Exception {

    }

    @Test
    public void testWithScope() throws Exception {

    }

    @Test
    public void testGetScope() throws Exception {

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

        // Ensure user1 can update/delete his own doc
        assertTrue(acl.hasPermission(userdoc1, UPDATE, user1));
        assertTrue(acl.hasPermission(userdoc1, DELETE, user1));

        // Ensure neither user1 or user2 can update/delete head's docs
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user1));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user1));
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user2));

        // Ensure neither user1 or user2 can update/delete each other's docs
        assertFalse(acl.hasPermission(userdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(userdoc1, DELETE, user2));
        assertFalse(acl.hasPermission(userdoc2, UPDATE, user1));
        assertFalse(acl.hasPermission(userdoc2, DELETE, user1));
    }
}
