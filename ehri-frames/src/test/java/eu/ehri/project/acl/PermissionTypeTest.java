package eu.ehri.project.acl;

import org.junit.Test;

import static eu.ehri.project.acl.PermissionType.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: mike
 */
public class PermissionTypeTest {
    @Test
    public void testContains() throws Exception {
        // Sanity checking...
        assertTrue(OWNER.contains(CREATE));
        assertTrue(OWNER.contains(UPDATE));
        assertTrue(OWNER.contains(DELETE));
        assertTrue(OWNER.contains(ANNOTATE));
        assertFalse(OWNER.contains(GRANT));

        assertFalse(CREATE.contains(UPDATE));
        assertFalse(CREATE.contains(OWNER));
    }
}
