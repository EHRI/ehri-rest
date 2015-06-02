package eu.ehri.project.views;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UserProfileViewsTest extends AbstractFixtureTest {

    private UserProfileViews views;
    private ActionManager am;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        views = new UserProfileViews(graph);
        am = new ActionManager(graph);
    }

    @Test
    public void testNoOpLogging() throws Exception {
        // If no items are given, we shouldn't log anything...
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        views.addWatching(invalidUser, Lists.<String>newArrayList(), invalidUser);
        assertEquals(latestEvent, am.getLatestGlobalEvent());
    }

    @Test
    public void testAddWatching() throws Exception {
        views.addWatching(invalidUser, Lists.newArrayList(item.getId()), invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.watch, latestEvent.getEventType());
    }

    @Test
    public void testRemoveWatching() throws Exception {
        invalidUser.addWatching(item);
        List<String> ids = Lists.newArrayList(item.getId());
        views.removeWatching(invalidUser, ids, invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unwatch, latestEvent.getEventType());
    }

    @Test
    public void testAddFollowers() throws Exception {
        views.addFollowers(invalidUser, Lists.newArrayList(validUser.getId()), invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.follow, latestEvent.getEventType());
    }

    @Test
    public void testRemoveFollowers() throws Exception {
        invalidUser.addFollowing(validUser);
        views.removeFollowers(invalidUser, Lists.newArrayList(validUser.getId()), invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unfollow, latestEvent.getEventType());
    }

    @Test
    public void testAddBlocked() throws Exception {
        views.addBlocked(invalidUser, Lists.newArrayList(validUser.getId()), invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.block, latestEvent.getEventType());
    }

    @Test
    public void testRemoveBlocked() throws Exception {
        invalidUser.addBlocked(validUser);
        views.removeBlocked(invalidUser, Lists.newArrayList(validUser.getId()), invalidUser);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unblock, latestEvent.getEventType());
    }
}