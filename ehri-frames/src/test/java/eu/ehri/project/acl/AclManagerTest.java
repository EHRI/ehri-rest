package eu.ehri.project.acl;

import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.test.utils.fixtures.FixtureLoader;
import eu.ehri.project.test.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.utils.GraphInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertTrue(new AclManager(graph).isAdmin(admin));
    }

    @Test
    public void testBelongsToAdmin() throws Exception {
        loader.loadTestData();
        UserProfile mike = manager.getFrame("mike", UserProfile.class);
        assertTrue(new AclManager(graph).belongsToAdmin(mike));
    }

    @Test
    public void testIsAnonymous() throws Exception {
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        Accessor anon = AnonymousAccessor.getInstance();
        AclManager acl = new AclManager(graph);
        assertFalse(acl.isAdmin(anon));
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

    }

    @Test
    public void testGetGlobalPermissions() throws Exception {

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
}
