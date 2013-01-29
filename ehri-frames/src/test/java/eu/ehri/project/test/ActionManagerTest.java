package eu.ehri.project.test;

import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.Action;
import eu.ehri.project.models.events.GlobalEvent;
import eu.ehri.project.models.events.ItemEvent;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;

public class ActionManagerTest extends AbstractFixtureTest {

    private ActionManager am;

    @Test
    public void testSystemNodeExists() {
        assertTrue(manager.exists(ActionManager.GLOBAL_EVENT_ROOT));
    }

    @Test
    public void testCorrectEventNodesAreCreated() throws DeserializationError, ValidationError {
        Bundle userBundle = Bundle.fromData(getTestUserBundle());
        UserProfile user = new BundleDAO(graph).create(userBundle, UserProfile.class);

        Bundle repoBundle = Bundle.fromData(getTestAgentBundle());
        Agent agent = new BundleDAO(graph).create(repoBundle, Agent.class);

        ActionManager am = new ActionManager(graph);
        ActionManager.EventContext ctx = am.logEvent(agent,
                graph.frame(validUser.asVertex(), Actioner.class), "Creating agent");

        GlobalEvent event = ctx.getGlobalEvent();

        // Check exactly one ItemEvent was created
        assertEquals(1, Iterables.count(event.getSubjects()));
        assertEquals(1, Iterables.count(event.getActioners()));

        assertEquals(1, Iterables.count(agent.getHistory()));
        assertEquals(1, Iterables.count(agent.getLatestEvent()));


        // Check the latest event in the list is the one we want...
        //GlobalEvent top = am.getLatestGlobalEvents().iterator().next();
//        assertEquals(top, event.getGlobalEvent());

        for (GlobalEvent ev : am.getLatestGlobalEvents()) {
            System.out.println(ev);
        }


    }
}
