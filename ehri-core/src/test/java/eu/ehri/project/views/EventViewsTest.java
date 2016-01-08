/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class EventViewsTest extends AbstractFixtureTest {

    private EventViews eventViews;
    private UserProfile user1;
    private UserProfile user2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        eventViews = new EventViews(graph);
        user1 = manager.getEntity("mike", UserProfile.class);
        user2 = manager.getEntity("tim", UserProfile.class);
    }

    @Test
    public void testList() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        String timestamp = ActionManager.getTimestamp();
        Thread.sleep(10);
        DocumentaryUnit doc2 = createItemWithIdentifier("bar", user1);

        Iterable<SystemEvent> fullList = eventViews.list(user1);
        assertEquals(2, Iterables.size(fullList));

        // Test user filter
        Iterable<SystemEvent> userList = eventViews
                .withUsers(user2.getId())
                .list(user1);
        assertEquals(0, Iterables.size(userList));

        List<SystemEvent> userList2 = Lists.newArrayList(eventViews
                .withUsers(user2.getId(), user1.getId())
                .list(user1));
        assertEquals(2, userList2.size());
        assertEquals(doc2, userList2.get(0).getFirstSubject());

        // Test time filter
        List<SystemEvent> toList = Lists.newArrayList(eventViews
                .to(timestamp)
                .list(user1));
        assertEquals(1, Iterables.size(toList));
        assertEquals(doc2, toList.get(0).getFirstSubject());

        List<SystemEvent> fromList = Lists.newArrayList(eventViews
                .from(timestamp)
                .list(user1));
        assertEquals(1, Iterables.size(fromList));
        assertEquals(doc1, fromList.get(0).getFirstSubject());

        // Test ID filter
        List<SystemEvent> idList = Lists.newArrayList(eventViews
                .withIds(doc1.getId())
                .list(user1));
        assertEquals(1, Iterables.size(idList));
        assertEquals(doc1, idList.get(0).getFirstSubject());

        // Test event type filter
        List<SystemEvent> evList = Lists.newArrayList(eventViews
                .withEventTypes(EventTypes.deletion)
                .list(user1));
        assertEquals(0, Iterables.size(evList));

        // Test entity type filter
        List<SystemEvent> etList = Lists.newArrayList(eventViews
                .withEntityClasses(EntityClass.HISTORICAL_AGENT)
                .list(user1));
        assertEquals(0, Iterables.size(etList));

        // Test paging...
        List<SystemEvent> eventPage = Lists.newArrayList(eventViews.withRange(1, 1).list(user1));
        assertEquals(1, eventPage.size());
        // events are temporally ordered, so the second item
        // in the queue will be the first thing created.
        assertEquals(doc1, eventPage.get(0).getFirstSubject());
    }

    @Test
    public void testListAsUserFollowing() throws Exception {
        createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        Thread.sleep(10);
        createItemWithIdentifier("bar", user1);

        EventViews followEvents = eventViews.withShowType(EventViews.ShowType.followed);
        List<SystemEvent> events = Lists
                .newArrayList(followEvents
                        .listAsUser(user2, user2));
        // Initially the list should
        // be empty because user2 does not follow user1
        assertTrue(events.isEmpty());
        user2.addFollowing(user1);
        List<SystemEvent> events2 = Lists
                .newArrayList(followEvents
                        .listAsUser(user2, user2));
        assertFalse(events2.isEmpty());
        assertEquals(2, events2.size());
    }

    @Test
    public void testAggregateAsUserFollowing() throws Exception {
        createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        Thread.sleep(10);
        createItemWithIdentifier("bar", user1);

        EventViews followEvents = eventViews.withShowType(EventViews.ShowType.followed);
        List<List<SystemEvent>> events = Lists
                .newArrayList(followEvents
                        .aggregateAsUser(user2, user2));
        // Initially the list should
        // be empty because user2 does not follow user1
        assertTrue(events.isEmpty());
        user2.addFollowing(user1);
        List<List<SystemEvent>> events2 = Lists
                .newArrayList(followEvents
                        .aggregateAsUser(user2, user2));
        assertFalse(events2.isEmpty());
        assertEquals(2, events2.size());
    }

    @Test
    public void testListByUser() throws Exception {
        List<SystemEvent> events = Lists
                .newArrayList(eventViews
                        .listByUser(user1, user2));
        assertEquals(0, events.size());

        createItemWithIdentifier("foo", user1);
        createItemWithIdentifier("bar", user1);

        List<SystemEvent> events2 = Lists
                .newArrayList(eventViews
                        .listByUser(user1, user2));
        assertEquals(2, events2.size());
    }

    @Test
    public void testEventAggregation() throws Exception {
        createItemWithIdentifier("foo", user1);
        DocumentaryUnit item = createItemWithIdentifier("bar", user1);
        updateItem(item, "test", "test1", user1);
        updateItem(item, "test", "test2", user1);

        Iterable<List<SystemEvent>> aggregate = eventViews
                .aggregate(user1);
        List<List<SystemEvent>> events = Lists.newArrayList(aggregate);
        // NB: The list is in most-recent order, so it should
        // be two aggregated update events, followed by two
        // non-aggregated creation events.
        assertEquals(3, events.size());
        assertEquals(2, events.get(0).size());
        assertEquals(1, events.get(1).size());
        assertEquals(1, events.get(2).size());
    }

    @Test
    public void testEventAggregationWithPagination() throws Exception {
        createItemWithIdentifier("foo", user1);
        DocumentaryUnit item = createItemWithIdentifier("bar", user1);
        updateItem(item, "test", "test1", user1);
        updateItem(item, "test", "test2", user1);

        Iterable<List<SystemEvent>> aggregate1 = eventViews.withRange(0, 2).aggregate(user1);
        Iterable<List<SystemEvent>> aggregate2 = eventViews.withRange(2, 2).aggregate(user1);
        List<List<SystemEvent>> events1 = Lists.newArrayList(aggregate1);
        List<List<SystemEvent>> events2 = Lists.newArrayList(aggregate2);
        assertEquals(2, events1.size());
        assertEquals(1, events2.size());
    }

    @Test
    public void testEventAggregationByUser() throws Exception {
        createItemWithIdentifier("foo", user1);
        DocumentaryUnit item = createItemWithIdentifier("bar", user1);
        updateItem(item, "test", "test1", user1);
        updateItem(item, "test", "test2", user2);

        Iterable<List<SystemEvent>> aggregate = eventViews
                .withAggregation(EventViews.Aggregation.user)
                .aggregate(user1);
        List<List<SystemEvent>> events = Lists.newArrayList(aggregate);
        assertEquals(2, events.size());
        assertEquals(1, events.get(0).size());
        assertEquals(3, events.get(1).size());
    }

    @Test
    public void testListAsUserWatching() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        Thread.sleep(10);
        createItemWithIdentifier("bar", user1);

        EventViews watchEvents = eventViews.withShowType(EventViews.ShowType.watched);
        List<SystemEvent> events = Lists
                .newArrayList(watchEvents
                        .listAsUser(user2, user2));
        // Initially the list should
        // be empty because user2 does not watch any items
        assertTrue(events.isEmpty());
        user2.addWatching(doc1);
        List<SystemEvent> events2 = Lists
                .newArrayList(watchEvents
                        .listAsUser(user2, user2));
        assertFalse(events2.isEmpty());
        assertEquals(1, events2.size());
        assertEquals(doc1, events2.get(0).getFirstSubject());
    }

    @Test
    public void testListForItem() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        List<SystemEvent> events = Lists.newArrayList(eventViews.listForItem(doc1, user1));
        assertEquals(1, events.size());
        assertEquals(doc1, events.get(0).getFirstSubject());
    }

    @Test
    public void testAggregateForItem() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        updateItem(doc1, "foo", "bar", user2);
        List<List<SystemEvent>> events = Lists
                .newArrayList(eventViews.aggregateForItem(doc1, user1));
        assertEquals(2, events.size());
        assertEquals(doc1, events.get(0).get(0).getFirstSubject());
        assertEquals(user2, events.get(0).get(0).getActioner());
        assertEquals(user1, events.get(1).get(0).getActioner());
    }

    private DocumentaryUnit createItemWithIdentifier(String id, UserProfile userProfile) throws Exception {
        LoggingCrudViews<DocumentaryUnit> docViews = new LoggingCrudViews<>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, id);
        return docViews.create(bundle, userProfile);
    }

    private DocumentaryUnit updateItem(DocumentaryUnit item, String key, String value, UserProfile user) throws
            Exception {
        Bundle bundle = new Serializer.Builder(graph).dependentOnly().build()
                .entityToBundle(item);
        Bundle bundle1 = bundle.withDataValue(key, value);
        Mutation<DocumentaryUnit> update = new LoggingCrudViews<>(graph, DocumentaryUnit.class)
                .update(bundle1, user);
        return update.getNode();
    }
}
