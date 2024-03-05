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
        views(basicUser).addWatching(basicUser.getId(), Lists.<String>newArrayList());
        assertEquals(latestEvent, am.getLatestGlobalEvent());
    }

    @Test
    public void testAddWatching() throws Exception {
        views(basicUser).addWatching(basicUser.getId(), Lists.newArrayList(item.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.watch, latestEvent.getEventType());
    }

    @Test
    public void testRemoveWatching() throws Exception {
        basicUser.addWatching(item);
        List<String> ids = Lists.newArrayList(item.getId());
        views(basicUser).removeWatching(basicUser.getId(), ids);
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.unwatch, latestEvent.getEventType());
    }

    @Test
    public void testAddFollowers() throws Exception {
        views(basicUser).addFollowers(basicUser.getId(), Lists.newArrayList(adminUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.follow, latestEvent.getEventType());
    }

    @Test
    public void testRemoveFollowers() throws Exception {
        basicUser.addFollowing(adminUser);
        views(basicUser).removeFollowers(basicUser.getId(), Lists.newArrayList(adminUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.unfollow, latestEvent.getEventType());
    }

    @Test
    public void testAddBlocked() throws Exception {
        views(basicUser).addBlocked(basicUser.getId(), Lists.newArrayList(adminUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.block, latestEvent.getEventType());
    }

    @Test
    public void testRemoveBlocked() throws Exception {
        basicUser.addBlocked(adminUser);
        views(basicUser).removeBlocked(basicUser.getId(), Lists.newArrayList(adminUser.getId()));
        SystemEvent latestEvent = am.getLatestGlobalEvent();
        assertEquals(basicUser, latestEvent.getActioner());
        assertEquals(EventTypes.unblock, latestEvent.getEventType());
    }
}