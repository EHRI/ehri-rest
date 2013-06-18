package eu.ehri.project.acl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: mike
 * Date: 30/05/13
 * Time: 20:06
 * To change this template use File | Settings | File Templates.
 */
public class GlobalPermissionSetTest {

    @Test
    public void testEquals() throws Exception {
        GlobalPermissionSet a = new GlobalPermissionSet();
        GlobalPermissionSet b = new GlobalPermissionSet();

        a.setContentType(ContentTypes.COUNTRY, PermissionType.CREATE, PermissionType.UPDATE);
        b.setContentType(ContentTypes.COUNTRY, PermissionType.UPDATE, PermissionType.CREATE);
        assertEquals(a, b);
    }
}
