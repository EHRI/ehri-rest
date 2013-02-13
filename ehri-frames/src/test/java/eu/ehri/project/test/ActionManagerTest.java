package eu.ehri.project.test;

import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import java.util.List;

import static org.junit.Assert.*;

public class ActionManagerTest extends AbstractFixtureTest {

    private ActionManager am;

    @Test
    public void testSystemNodeExists() {
        assertTrue(manager.exists(ActionManager.GLOBAL_EVENT_ROOT));
    }

    @Test
    public void testCorrectEventNodesAreCreated() throws DeserializationError, ValidationError {
        ActionManager am = new ActionManager(graph);
        // Create a user and log it
        Bundle userBundle = Bundle.fromData(getTestUserBundle());
        UserProfile user = new BundleDAO(graph).create(userBundle, UserProfile.class);
        SystemEvent first = am.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class), "Creating user").getSystemEvent();

        // Create an agent and log that too...
        Bundle repoBundle = Bundle.fromData(getTestAgentBundle());
        Agent agent = new BundleDAO(graph).create(repoBundle, Agent.class);

        SystemEvent second = am.logEvent(agent,
                graph.frame(validUser.asVertex(), Actioner.class), "Creating agent")
                .getSystemEvent();

        // Check exactly one Event was created
        assertEquals(1, Iterables.count(second.getSubjects()));
        assertEquals(1, Iterables.count(second.getActioners()));

        assertEquals(1, Iterables.count(agent.getHistory()));
        assertEquals(1, Iterables.count(agent.getLatestEvent()));

        // Check the latest event in the list is the one we want...
        SystemEvent top = am.getLatestGlobalEvents().iterator().next();
        assertEquals(top, second);

        // Check the full list of system events contains both items
        List<SystemEvent> events = toList(am.getLatestGlobalEvents());
        assertEquals(2, events.size());
        assertEquals(second, events.get(0));
        assertEquals(first, events.get(1));
    }
}
