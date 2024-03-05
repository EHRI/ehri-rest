package eu.ehri.project.models.events;

import com.google.common.collect.Iterators;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class SystemEventQueueTest extends AbstractFixtureTest {

    @Test
    public void testGetLatestEvent() throws Exception {
        SystemEventQueue queue = manager.getEntity(ActionManager.GLOBAL_EVENT_ROOT, SystemEventQueue.class);
        assertNull(queue.getLatestEvent());
        ActionManager.EventContext ctx = new ActionManager(graph)
                .newEventContext(adminUser, adminUser.as(Actioner.class), EventTypes.creation);
        SystemEvent commit = ctx.commit();
        assertEquals(commit, queue.getLatestEvent());
    }

    @Test
    public void testGetSystemEvents() throws Exception {
        SystemEventQueue queue = manager.getEntity(ActionManager.GLOBAL_EVENT_ROOT, SystemEventQueue.class);
        new ActionManager(graph)
                .newEventContext(adminUser, adminUser.as(Actioner.class), EventTypes.creation)
                .commit();
        assertEquals(1, Iterators.size(queue.getSystemEvents().iterator()));
    }
}