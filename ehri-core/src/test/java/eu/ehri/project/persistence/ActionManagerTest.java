/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.persistence;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

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
        UserProfile user = new BundleManager(graph).create(userBundle, UserProfile.class);
        ActionManager.EventContext ctx1 = am.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        SystemEvent first = ctx1.commit();

        // Create a repository and log that too...
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = new BundleManager(graph).create(repoBundle, Repository.class);

        ActionManager.EventContext ctx2 = am.newEventContext(repository,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        assertEquals(EventTypes.creation, ctx2.getEventType());
        SystemEvent second = ctx2.commit();

        // Check exactly one Event was created
        assertEquals(1, Iterables.size(second.getSubjects()));
        // Check item cache is correct...
        assertEquals(1, second.subjectCount());
        assertNotNull(second.getActioner());

        // Check the user is correctly linked
        assertEquals(validUser, second.getActioner());

        assertEquals(1, Iterables.size(repository.getHistory()));
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
        Repository r1 = manager.getEntity("r1", Repository.class);
        ActionManager am = new ActionManager(graph, r1);

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit doc = new BundleManager(graph).create(docBundle, DocumentaryUnit.class);
        ActionManager.EventContext ctx = am.newEventContext(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        SystemEvent log = ctx.commit();
        assertNotNull(log.getEventScope());
        assertEquals(r1.asVertex(), log.getEventScope().asVertex());
    }

    @Test
    public void testCreatingNewEventDoesNotTouchGraph() throws Exception {
        ActionManager am = new ActionManager(graph);
        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        BundleManager dao = new BundleManager(graph);
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        int nodesBefore = getNodeCount(graph);
        int edgesBefore = getEdgeCount(graph);
        ActionManager.EventContext ctx1 = am.newEventContext(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        assertEquals(nodesBefore, getNodeCount(graph));
        assertEquals(edgesBefore, getEdgeCount(graph));
        ctx1.commit();
        // Should have created:
        //  - 1 more event
        //  - 2 more event links
        //  - 5 more edges
        assertEquals(nodesBefore + 3, getNodeCount(graph));
        assertEquals(edgesBefore + 5, getEdgeCount(graph));
    }

    @Test
    public void testCreatingVersions() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        ActionManager am = new ActionManager(graph, r1);

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        BundleManager dao = new BundleManager(graph);
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        ActionManager.EventContext ctx1 = am.newEventContext(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        ctx1.commit();
        assertNull(doc.getPriorVersion());
        Mutation<DocumentaryUnit> update = dao.update(docBundle
                .withId(doc.getId())
                .withDataValue("identifier", "changed"), DocumentaryUnit.class);
        ActionManager.EventContext ctx2 = am.newEventContext(doc,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).createVersion(doc, docBundle);
        SystemEvent event = ctx2.commit();
        assertTrue(update.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        assertEquals(1, Iterables.size(event.getPriorVersions()));
        Version version = event.getPriorVersions().iterator().next();
        assertNotNull(doc.getPriorVersion());
        assertEquals(version, doc.getPriorVersion());

        // Create another event and ensure versions are ordered correctly
        dao.update(docBundle
                .withId(doc.getId())
                .withDataValue("identifier", "changed-again"), DocumentaryUnit.class);
        SystemEvent event2 = ctx2.commit();
        assertEquals(1, Iterables.size(event2.getPriorVersions()));
        assertEquals(2, Iterables.size(doc.getAllPriorVersions()));
    }
}
