package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GroupTest extends AbstractFixtureTest {

    @Test
    public void testAdminInitializedProperty() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // Both identifier and name should initialize to 'admin'
        assertEquals(admin.getIdentifier(), Group.ADMIN_GROUP_IDENTIFIER);
        assertEquals(admin.getName(), Group.ADMIN_GROUP_NAME);
    }

    @Test
    public void testAddMember() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        List<Accessor> list = Lists.newArrayList(admin.getMembers());
        long numMembers = list.size();
        // Adding same member twice should affect member count - it should be idempotent
        admin.addMember(validUser);
        assertEquals(numMembers, Iterables.size(admin.getMembers()));
        admin.addMember(invalidUser);
        assertEquals(numMembers + 1L, Iterables.size(admin.getMembers()));
    }

    @Test
    public void testRemoveMember() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        List<Accessor> list = Lists.newArrayList(admin.getMembers());
        long numMembers = list.size();
        // Adding same member twice should affect member count - it should be idempotent
        admin.removeMember(invalidUser);
        assertEquals(numMembers, Iterables.size(admin.getMembers()));
        admin.removeMember(validUser);
        assertEquals(numMembers - 1L, Iterables.size(admin.getMembers()));
    }

    @Test
    public void testGetAllUserProfileMembers() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // All users should be mike, veerle, tim (inherited)
        List<?> userProfileList = toList(admin.getAllUserProfileMembers());
        assertEquals(3, userProfileList.size());
    }
}
