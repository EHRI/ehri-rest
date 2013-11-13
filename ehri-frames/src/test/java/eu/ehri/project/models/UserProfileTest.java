package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: mikebryant
 */
public class UserProfileTest extends AbstractFixtureTest {
    @Test
    public void testGetGroups() throws Exception {
        assertTrue(validUser.getGroups().iterator().hasNext());
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        assertTrue(Iterables.contains(validUser.getGroups(), admin));
    }

    @Test
    public void testGetFollowing() throws Exception {
        UserProfile follower = manager.getFrame("reto", UserProfile.class);
        assertTrue(Iterables.isEmpty(validUser.getFollowing()));
        validUser.addFollowing(follower);
        assertFalse(Iterables.isEmpty(validUser.getFollowing()));
        assertTrue(Iterables.contains(validUser.getFollowing(), follower));
        validUser.removeFollowing(follower);
        assertTrue(Iterables.isEmpty(validUser.getFollowing()));
    }

    @Test
    public void testIsFollowing() throws Exception {
        UserProfile follower = manager.getFrame("reto", UserProfile.class);
        assertFalse(follower.isFollowing(validUser));
        follower.addFollowing(validUser);
        assertTrue(follower.isFollowing(validUser));
        assertTrue(validUser.isFollower(follower));
        assertFalse(validUser.isFollowing(follower));
    }
}
