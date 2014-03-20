package eu.ehri.project.acl;

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
public class InheritedGlobalPermissionSetTest extends AbstractFixtureTest {

    private AclManager aclManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        aclManager = new AclManager(graph);
    }

    @Test
    public void testHas() throws Exception {
        InheritedGlobalPermissionSet permissions1
                = aclManager.getInheritedGlobalPermissions(invalidUser);
        assertFalse(permissions1.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE));
        assertFalse(permissions1.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE));

        InheritedGlobalPermissionSet permissions2
                = aclManager.getInheritedGlobalPermissions(validUser);
        assertTrue(permissions2.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE));
        assertTrue(permissions2.has(ContentTypes.DOCUMENTARY_UNIT, PermissionType.DELETE));
    }

    @Test
    public void testSerialize() throws Exception {
        InheritedGlobalPermissionSet permissions
                = aclManager.getInheritedGlobalPermissions(invalidUser);
        List<Map<String,GlobalPermissionSet>> list = permissions.serialize();
        assertEquals(2L, list.size());
    }
}
