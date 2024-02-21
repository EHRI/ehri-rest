package eu.ehri.project.api;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class UserProfilesApiTest extends AbstractFixtureTest {

    private ActionManager am;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        am = new ActionManager(graph);
    }

    private UserProfilesApi views(Accessor accessor) {
        return loggingApi(accessor).userProfiles();
    }

    @Test
    public void testNoOpLogging() throws Exception {
        // If no items are given, we shouldn't log anything...
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        views(invalidUser).addWatching(invalidUser.getId(), Lists.<String>newArrayList());
        assertEquals(latestEvent, am.getLatestGlobalEvent());
    }

    @Test
    public void testAddWatching() throws Exception {
        views(invalidUser).addWatching(invalidUser.getId(), Lists.newArrayList(item.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.watch, latestEvent.getEventType());
    }

    @Test
    public void testRemoveWatching() throws Exception {
        invalidUser.addWatching(item);
        List<String> ids = Lists.newArrayList(item.getId());
        views(invalidUser).removeWatching(invalidUser.getId(), ids);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unwatch, latestEvent.getEventType());
    }

    @Test
    public void testAddFollowers() throws Exception {
        views(invalidUser).addFollowers(invalidUser.getId(), Lists.newArrayList(validUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.follow, latestEvent.getEventType());
    }

    @Test
    public void testRemoveFollowers() throws Exception {
        invalidUser.addFollowing(validUser);
        views(invalidUser).removeFollowers(invalidUser.getId(), Lists.newArrayList(validUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unfollow, latestEvent.getEventType());
    }

    @Test
    public void testAddBlocked() throws Exception {
        views(invalidUser).addBlocked(invalidUser.getId(), Lists.newArrayList(validUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.block, latestEvent.getEventType());
    }

    @Test
    public void testRemoveBlocked() throws Exception {
        invalidUser.addBlocked(validUser);
        views(invalidUser).removeBlocked(invalidUser.getId(), Lists.newArrayList(validUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(invalidUser, latestEvent.getActioner());
        assertEquals(EventTypes.unblock, latestEvent.getEventType());
    }
}