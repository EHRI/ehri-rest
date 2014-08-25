package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EventViewsTest extends AbstractFixtureTest {

    private EventViews eventViews;
    private Query<SystemEvent> query;
    private UserProfile user1;
    private UserProfile user2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        eventViews = new EventViews(graph);
        query = new Query<SystemEvent>(graph, SystemEvent.class);
        user1 = manager.getFrame("mike", UserProfile.class);
        user2 = manager.getFrame("tim", UserProfile.class);
    }

    @Test
    public void testList() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        String timestamp = ActionManager.getTimestamp();
        Thread.sleep(10);
        DocumentaryUnit doc2 = createItemWithIdentifier("bar", user1);

        Iterable<SystemEvent> fullList = eventViews.list(query, user1);
        assertEquals(2, Iterables.size(fullList));

        // Test user filter
        Iterable<SystemEvent> userList = eventViews
                .withUsers(user2.getId())
                .list(query, user1);
        assertEquals(0, Iterables.size(userList));

        // Test time filter
        List<SystemEvent> toList = Lists.newArrayList(eventViews
                .to(timestamp)
                .list(query, user1));
        assertEquals(1, Iterables.size(toList));
        assertEquals(doc1, toList.get(0).getFirstSubject());

        List<SystemEvent> fromList = Lists.newArrayList(eventViews
                .from(timestamp)
                .list(query, user1));
        assertEquals(1, Iterables.size(fromList));
        assertEquals(doc2, fromList.get(0).getFirstSubject());

        // Test ID filter
        List<SystemEvent> idList = Lists.newArrayList(eventViews
                .withIds(doc1.getId())
                .list(query, user1));
        assertEquals(1, Iterables.size(idList));
        assertEquals(doc1, idList.get(0).getFirstSubject());

        // Test event type filter
        List<SystemEvent> evList = Lists.newArrayList(eventViews
                .withEventTypes(EventTypes.deletion)
                .list(query, user1));
        assertEquals(0, Iterables.size(evList));

        // Test entity type filter
        List<SystemEvent> etList = Lists.newArrayList(eventViews
                .withEntityClasses(EntityClass.HISTORICAL_AGENT)
                .list(query, user1));
        assertEquals(0, Iterables.size(etList));

        // Test paging...
        List<SystemEvent> eventPage = Lists.newArrayList(eventViews.list(query
                .setCount(1).setPage(2), user1));
        assertEquals(1, eventPage.size());
        // events are temporally ordered, so the second item
        // in the queue will be the first thing created.
        assertEquals(doc1, eventPage.get(0).getFirstSubject());
    }

    @Test
    public void testListAsUserFollowing() throws Exception {
        createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        String timestamp = ActionManager.getTimestamp();
        Thread.sleep(10);
        createItemWithIdentifier("bar", user1);

        EventViews followEvents = eventViews.withShowType(EventViews.ShowType.followed);
        List<SystemEvent> events = Lists
                .newArrayList(followEvents
                        .listAsUser(query, user2, user2));
        // Initially the list should
        // be empty because user2 does not follow user1
        assertTrue(events.isEmpty());
        user2.addFollowing(user1);
        List<SystemEvent> events2 = Lists
                .newArrayList(followEvents
                        .listAsUser(query, user2, user2));
        assertFalse(events2.isEmpty());
        assertEquals(2, events2.size());
    }

    @Test
    public void testListAsUserWatching() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        String timestamp = ActionManager.getTimestamp();
        Thread.sleep(10);
        DocumentaryUnit doc2 = createItemWithIdentifier("bar", user1);

        EventViews watchEvents = eventViews.withShowType(EventViews.ShowType.watched);
        List<SystemEvent> events = Lists
                .newArrayList(watchEvents
                        .listAsUser(query, user2, user2));
        // Initially the list should
        // be empty because user2 does not watch any items
        assertTrue(events.isEmpty());
        user2.addWatching(doc1);
        List<SystemEvent> events2 = Lists
                .newArrayList(watchEvents
                        .listAsUser(query, user2, user2));
        assertFalse(events2.isEmpty());
        assertEquals(1, events2.size());
        assertEquals(doc1, events2.get(0).getFirstSubject());
    }

    public DocumentaryUnit createItemWithIdentifier(String id, UserProfile userProfile) throws Exception {
        LoggingCrudViews<DocumentaryUnit> docViews = new LoggingCrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, id);
        return docViews.create(bundle, userProfile);
    }
}
