package eu.ehri.project.models;

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

public class GroupTest extends AbstractFixtureTest {

    @Test
    public void testAdminInitializedProperty() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // Both identifier and name should initialize to 'admin'
        assertEquals(admin.getIdentifier(), Group.ADMIN_GROUP_IDENTIFIER);
        assertEquals(admin.getName(), Group.ADMIN_GROUP_NAME);
    }

    @Test
    public void testGetAllUserProfileMembers() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // All users should be mike, veerle, tim (inherited)
        List<?> userProfileList = toList(admin.getAllUserProfileMembers());
        assertEquals(3, userProfileList.size());
    }
}
