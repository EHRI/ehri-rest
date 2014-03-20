package eu.ehri.project.acl;

import eu.ehri.project.models.Repository;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InheritedItemPermissionSetTest extends AbstractFixtureTest {

    private AclManager aclManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        aclManager = new AclManager(graph);
    }

    @Test
    public void testHas() throws Exception {
        Repository r2 = manager.getFrame("r2", Repository.class);
        InheritedItemPermissionSet permissionSet
                = aclManager.getInheritedItemPermissions(r2, invalidUser);
        assertFalse(permissionSet.has(PermissionType.CREATE));
        assertTrue(permissionSet.has(PermissionType.UPDATE));
        assertFalse(permissionSet.has(PermissionType.DELETE));
    }

    @Test
    public void testSerialize() throws Exception {
        InheritedItemPermissionSet permissions
                = aclManager.getInheritedItemPermissions(manager.getFrame("r2", Repository.class), invalidUser
        );
        List<Map<String,List<PermissionType>>> serialized = permissions.serialize();
        // Should contain Reto and Kcl
        assertEquals(2L, serialized.size());
    }
}
