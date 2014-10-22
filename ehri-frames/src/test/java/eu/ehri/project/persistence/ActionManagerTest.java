package eu.ehri.project.persistence;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        ActionManager.EventContext eventContext = am.logEvent(repository,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        assertEquals(EventTypes.creation, eventContext.getEventType());
        SystemEvent second = eventContext
                .getSystemEvent();

        // Check exactly one Event was created
        assertEquals(1, Iterables.count(second.getSubjects()));
        // Check item cache is correct...
        assertEquals(1L, second.asVertex().getProperty(ItemHolder.CHILD_COUNT));
        assertNotNull(second.getActioner());

        // Check the user is correctly linked
        assertEquals(validUser, second.getActioner());

        assertEquals(1, Iterables.count(repository.getHistory()));
        assertNotNull(repository.getLatestEvent());

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

    @Test
    public void testCreatingVersions() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        ActionManager am = new ActionManager(graph, r1);

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        BundleDAO dao = new BundleDAO(graph);
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        am.logEvent(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();
        assertNull(doc.getPriorVersion());
        Mutation<DocumentaryUnit> update = dao.update(docBundle
                .withId(doc.getId())
                .withDataValue("identifier", "changed"), DocumentaryUnit.class);
        SystemEvent event = am.logEvent(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).createVersion(doc, docBundle)
                .getSystemEvent();
        assertTrue(update.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        assertEquals(1, Iterables.count(event.getPriorVersions()));
        Version version = event.getPriorVersions().iterator().next();
        assertNotNull(doc.getPriorVersion());
        assertEquals(version, doc.getPriorVersion());

        // Create another event and ensure versions are ordered correctly
        dao.update(docBundle
                .withId(doc.getId())
                .withDataValue("identifier", "changed-again"), DocumentaryUnit.class);
        SystemEvent event2 = am.logEvent(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).createVersion(doc, docBundle)
                .getSystemEvent();
        assertEquals(1, Iterables.count(event2.getPriorVersions()));
        assertEquals(2, Iterables.count(doc.getAllPriorVersions()));
    }
}
