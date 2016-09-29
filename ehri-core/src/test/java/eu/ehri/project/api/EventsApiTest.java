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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class EventsApiTest extends AbstractFixtureTest {

    private UserProfile user1;
    private UserProfile user2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        user1 = manager.getEntity("mike", UserProfile.class);
        user2 = manager.getEntity("tim", UserProfile.class);
    }

    public EventsApi events(Accessor accessor) {
        return api(accessor).events();
    }

    @Test
    public void testList() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        String timestamp = ActionManager.getTimestamp();
        Thread.sleep(10);
        DocumentaryUnit doc2 = createItemWithIdentifier("bar", user1);

        Iterable<SystemEvent> fullList = events(user1).list();
        assertEquals(2, Iterables.size(fullList));

        // Test user filter
        Iterable<SystemEvent> userList = events(user1)
                .withUsers(user2.getId())
                .list();
        assertEquals(0, Iterables.size(userList));

        List<SystemEvent> userList2 = Lists.newArrayList(events(user1)
                .withUsers(user2.getId(), user1.getId())
                .list());
        assertEquals(2, userList2.size());
        assertEquals(doc2, userList2.get(0).getFirstSubject());

        // Test time filter
        List<SystemEvent> toList = Lists.newArrayList(events(user1)
                .to(timestamp)
                .list());
        assertEquals(1, Iterables.size(toList));
        assertEquals(doc2, toList.get(0).getFirstSubject());

        List<SystemEvent> fromList = Lists.newArrayList(events(user1)
                .from(timestamp)
                .list());
        assertEquals(1, Iterables.size(fromList));
        assertEquals(doc1, fromList.get(0).getFirstSubject());

        // Test ID filter
        List<SystemEvent> idList = Lists.newArrayList(events(user1)
                .withIds(doc1.getId())
                .list());
        assertEquals(1, Iterables.size(idList));
        assertEquals(doc1, idList.get(0).getFirstSubject());

        // Test event type filter
        List<SystemEvent> evList = Lists.newArrayList(events(user1)
                .withEventTypes(EventTypes.deletion)
                .list());
        assertEquals(0, Iterables.size(evList));

        // Test entity type filter
        List<SystemEvent> etList = Lists.newArrayList(events(user1)
                .withEntityClasses(EntityClass.HISTORICAL_AGENT)
                .list());
        assertEquals(0, Iterables.size(etList));

        // Test paging...
        List<SystemEvent> eventPage = Lists.newArrayList(events(user1).withRange(1, 1).list());
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

        EventsApi followEvents = events(user2).withShowType(EventsApi.ShowType.followed);
        List<SystemEvent> events = Lists
                .newArrayList(followEvents
                        .listAsUser(user2));
        // Initially the list should
        // be empty because user2 does not follow user1
        assertTrue(events.isEmpty());
        user2.addFollowing(user1);
        List<SystemEvent> events2 = Lists
                .newArrayList(followEvents
                        .listAsUser(user2));
        assertFalse(events2.isEmpty());
        assertEquals(2, events2.size());
    }

    @Test
    public void testAggregateAsUserFollowing() throws Exception {
        createItemWithIdentifier("foo", user1);
        Thread.sleep(10);
        Thread.sleep(10);
        createItemWithIdentifier("bar", user1);

        EventsApi followEvents = events(user2).withShowType(EventsApi.ShowType.followed);
        List<List<SystemEvent>> events = Lists
                .newArrayList(followEvents
                        .aggregateAsUser(user2));
        // Initially the list should
        // be empty because user2 does not follow user1
        assertTrue(events.isEmpty());
        user2.addFollowing(user1);
        List<List<SystemEvent>> events2 = Lists
                .newArrayList(followEvents
                        .aggregateAsUser(user2));
        assertFalse(events2.isEmpty());
        assertEquals(2, events2.size());
    }

    @Test
    public void testListByUser() throws Exception {
        List<SystemEvent> events = Lists
                .newArrayList(events(user2)
                        .listByUser(user1));
        assertEquals(0, events.size());

        createItemWithIdentifier("foo", user1);
        createItemWithIdentifier("bar", user1);

        List<SystemEvent> events2 = Lists
                .newArrayList(events(user2)
                        .listByUser(user1));
        assertEquals(2, events2.size());
    }

    @Test
    public void testEventAggregation() throws Exception {
        createItemWithIdentifier("foo", user1);
        DocumentaryUnit item = createItemWithIdentifier("bar", user1);
        updateItem(item, "test", "test1", user1);
        updateItem(item, "test", "test2", user1);

        Iterable<List<SystemEvent>> aggregate = events(user1).aggregate();
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

        Iterable<List<SystemEvent>> aggregate1 = events(user1).withRange(0, 2).aggregate();
        Iterable<List<SystemEvent>> aggregate2 = events(user1).withRange(2, 2).aggregate();
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

        Iterable<List<SystemEvent>> aggregate = events(user1)
                .withAggregation(EventsApi.Aggregation.user)
                .aggregate();
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

        EventsApi watchEvents = events(user2).withShowType(EventsApi.ShowType.watched);
        List<SystemEvent> events = Lists
                .newArrayList(watchEvents
                        .listAsUser(user2));
        // Initially the list should
        // be empty because user2 does not watch any items
        assertTrue(events.isEmpty());
        user2.addWatching(doc1);
        List<SystemEvent> events2 = Lists
                .newArrayList(watchEvents
                        .listAsUser(user2));
        assertFalse(events2.isEmpty());
        assertEquals(1, events2.size());
        assertEquals(doc1, events2.get(0).getFirstSubject());
    }

    @Test
    public void testListForItem() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        List<SystemEvent> events = Lists.newArrayList(events(user1).listForItem(doc1));
        assertEquals(1, events.size());
        assertEquals(doc1, events.get(0).getFirstSubject());
    }

    @Test
    public void testAggregateForItem() throws Exception {
        DocumentaryUnit doc1 = createItemWithIdentifier("foo", user1);
        updateItem(doc1, "foo", "bar", user2);
        List<List<SystemEvent>> events = Lists
                .newArrayList(events(user1).aggregateForItem(doc1));
        assertEquals(2, events.size());
        assertEquals(doc1, events.get(0).get(0).getFirstSubject());
        assertEquals(user2, events.get(0).get(0).getActioner());
        assertEquals(user1, events.get(1).get(0).getActioner());
    }

    private DocumentaryUnit createItemWithIdentifier(String id, UserProfile userProfile) throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, id);
        return loggingApi(userProfile).create(bundle, DocumentaryUnit.class);
    }

    private DocumentaryUnit updateItem(DocumentaryUnit item, String key, String value, UserProfile user) throws
            Exception {
        Bundle bundle = new Serializer.Builder(graph).dependentOnly().build()
                .entityToBundle(item);
        Bundle bundle1 = bundle.withDataValue(key, value);
        return loggingApi(user).update(bundle1, DocumentaryUnit.class).getNode();
    }
}
