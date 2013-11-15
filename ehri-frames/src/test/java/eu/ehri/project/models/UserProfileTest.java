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
    public void testFollowing() throws Exception {
        UserProfile follower = manager.getFrame("reto", UserProfile.class);
        assertTrue(Iterables.isEmpty(validUser.getFollowing()));
        validUser.addFollowing(follower);
        // Get count caching
        assertEquals(1L, validUser.asVertex().getProperty(UserProfile.FOLLOWING_COUNT));
        assertEquals(1L, follower.asVertex().getProperty(UserProfile.FOLLOWER_COUNT));
        assertFalse(Iterables.isEmpty(validUser.getFollowing()));
        assertTrue(Iterables.contains(validUser.getFollowing(), follower));
        validUser.removeFollowing(follower);
        assertTrue(Iterables.isEmpty(validUser.getFollowing()));
        assertEquals(0L, validUser.asVertex().getProperty(UserProfile.FOLLOWING_COUNT));
        assertEquals(0L, follower.asVertex().getProperty(UserProfile.FOLLOWER_COUNT));
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

    @Test
    public void testDuplicateWatches() throws Exception {
        UserProfile follower = manager.getFrame("reto", UserProfile.class);
        assertFalse(follower.isFollowing(validUser));
        // Do this twice and ensure the follower count isn't altered...
        follower.addFollowing(validUser);
        follower.addFollowing(validUser);
        assertEquals(1L, Iterables.size(follower.getFollowing()));
    }

    @Test
    public void testWatching() throws Exception {
        DocumentaryUnit watched = manager.getFrame("c1", DocumentaryUnit.class);
        assertFalse(validUser.isWatching(watched));
        validUser.addWatching(watched);
        assertTrue(validUser.isWatching(watched));
        assertEquals(1L, validUser.asVertex().getProperty(UserProfile.WATCHING_COUNT));
        assertEquals(1L, watched.asVertex().getProperty(UserProfile.WATCHED_COUNT));
        assertTrue(Iterables.contains(validUser.getWatching(), watched));
        validUser.removeWatching(watched);
        assertFalse(validUser.isWatching(watched));
        assertEquals(0L, validUser.asVertex().getProperty(UserProfile.WATCHING_COUNT));
        assertEquals(0L, watched.asVertex().getProperty(UserProfile.WATCHED_COUNT));
    }
}
