package eu.ehri.project.test;

import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: michaelb
 * Date: 05/02/13
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
public class GroupTest extends AbstractFixtureTest {
    @Test
    public void testGetAllUserProfileMembers() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // All users should be mike, veerle, tim (inherited)
        List<UserProfile> userProfileList = toList(admin.getAllUserProfileMembers());
        assertEquals(3, userProfileList.size());
    }
}
