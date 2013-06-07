package eu.ehri.project.persistance;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import java.util.List;

import static org.junit.Assert.*;

public class ActionManagerTest extends AbstractFixtureTest {

    @Test
    public void testSystemNodeExists() {
        assertTrue(manager.exists(ActionManager.GLOBAL_EVENT_ROOT));
    }

    @Test
    public void testCorrectEventNodesAreCreated() throws DeserializationError, ValidationError {
        ActionManager am = new ActionManager(graph);
        // Create a user and log it
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = new BundleDAO(graph).create(userBundle, UserProfile.class);
        SystemEvent first = am.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();

        // Create a repository and log that too...
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = new BundleDAO(graph).create(repoBundle, Repository.class);

        SystemEvent second = am.logEvent(repository,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation)
                .getSystemEvent();

        // Check exactly one Event was created
        assertEquals(1, Iterables.count(second.getSubjects()));
        assertEquals(1, Iterables.count(second.getActioners()));

        assertEquals(1, Iterables.count(repository.getHistory()));
        assertEquals(1, Iterables.count(repository.getLatestEvent()));

        // Check the latest event in the list is the one we want...
        SystemEvent top = am.getLatestGlobalEvents().iterator().next();
        assertEquals(top, second);

        // Check the full list of system events contains both items
        List<SystemEvent> events = toList(am.getLatestGlobalEvents());
        assertEquals(2, events.size());
        assertEquals(second, events.get(0));
        assertEquals(first, events.get(1));
    }

    @Test
    public void testEventsHaveCorrectScope() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        ActionManager am = new ActionManager(graph, r1);

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit doc = new BundleDAO(graph).create(docBundle, DocumentaryUnit.class);
        SystemEvent log = am.logEvent(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();
        assertNotNull(log.getEventScope());
        assertEquals(r1.asVertex(), log.getEventScope().asVertex());
    }
}
