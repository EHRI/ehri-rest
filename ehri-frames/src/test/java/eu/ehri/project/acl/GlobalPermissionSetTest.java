package eu.ehri.project.acl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobalPermissionSetTest {

    @Test
    public void testEquals() throws Exception {
        GlobalPermissionSet.Builder a = GlobalPermissionSet.newBuilder();
        GlobalPermissionSet.Builder b = GlobalPermissionSet.newBuilder();

        a.set(ContentTypes.COUNTRY, PermissionType.CREATE, PermissionType.UPDATE);
        b.set(ContentTypes.COUNTRY, PermissionType.UPDATE, PermissionType.CREATE);
        GlobalPermissionSet setA = a.build();
        GlobalPermissionSet setB = b.build();
        // Test ordering doesn't matter
        assertEquals(setA, setB);
        assertTrue(setA.has(ContentTypes.COUNTRY, PermissionType.CREATE));
        assertTrue(setA.has(ContentTypes.COUNTRY, PermissionType.UPDATE));
        assertFalse(setA.has(ContentTypes.COUNTRY, PermissionType.DELETE));
    }
}
