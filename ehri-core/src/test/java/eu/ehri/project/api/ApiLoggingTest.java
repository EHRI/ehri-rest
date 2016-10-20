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
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApiLoggingTest extends AbstractFixtureTest {

    @Test
    public void testUpdate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(validUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        String newName = TestData.TEST_COLLECTION_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(unit.getId()).withDataValue("name", newName);

        DocumentaryUnit changedUnit = api(validUser).update(newBundle, DocumentaryUnit.class).getNode();
        assertEquals(newName, changedUnit.getProperty("name"));
        assertTrue(changedUnit.getDescriptions().iterator().hasNext());
        DocumentaryUnitDescription desc = graph.frame(
                changedUnit.getDescriptions().iterator().next().asVertex(),
                DocumentaryUnitDescription.class);

        // Check the nested item was created correctly
        DatePeriod datePeriod = desc.getDatePeriods().iterator().next();
        assertTrue(datePeriod != null);
        assertEquals(TestData.TEST_START_DATE, datePeriod.getStartDate());

        // And that the reverse relationship works.
        assertEquals(desc.asVertex(), datePeriod.getEntity().asVertex());
    }

    @Test
    public void testUserUpdate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = loggingApi(validUser).create(bundle, UserProfile.class);
        assertEquals(TestData.TEST_USER_NAME, user.getName());

        String newName = TestData.TEST_USER_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(user.getId()).withDataValue("name", newName);

        UserProfile changedUser = loggingApi(validUser).update(newBundle, UserProfile.class).getNode();
        assertEquals(newName, changedUser.getName());

        // Check we have an audit action.
        assertNotNull(changedUser.getLatestEvent());
        // FIXME: getLatestAction() should return a single item, but due to
        // a current (2.2.0) limitation in frames' @GremlinGroovy mechanism
        // it can't
        assertEquals(1, Iterables.size(validUser.getLatestAction()));
        SystemEvent event = Iterables.getFirst(validUser.getLatestAction(), null);
        assertNotNull(event);
        assertNotNull(event.getFirstSubject());
        assertEquals(changedUser.asVertex(), event.getFirstSubject().asVertex());
        assertTrue(changedUser.getHistory().iterator().hasNext());

        // We should have exactly two actions now; one for create, one for
        // update...
        List<SystemEvent> events = toList(changedUser.getHistory());
        assertEquals(2, events.size());
        // We should have the right subject on the actionEvent
        assertTrue(events.get(0).getSubjects().iterator().hasNext());
        assertTrue(events.get(1).getSubjects().iterator().hasNext());

        assertEquals(1, Iterables.size(events.get(0).getSubjects()));
        assertEquals(1, Iterables.size(events.get(1).getSubjects()));

        assertEquals(changedUser.asVertex(), events.get(0)
                .getSubjects().iterator().next().asVertex());
        assertEquals(changedUser.asVertex(), events.get(1)
                .getSubjects().iterator().next().asVertex());
        try {
            System.out.println(new Serializer(graph).entityToBundle(events.get(0)));
        } catch (SerializationError serializationError) {
            serializationError.printStackTrace();
        }
    }

    @Test
    public void testDelete() throws PermissionDenied, ValidationError,
            SerializationError, ItemNotFound {
        int shouldDelete = 1;
        int origActionCount = toList(validUser.getHistory()).size();

        // FIXME: Surely there's a better way of doing this???
        Iterator<Description> descIter = item.getDescriptions().iterator();
        for (; descIter.hasNext(); shouldDelete++) {
            DocumentaryUnitDescription d = graph.frame(descIter.next().asVertex(), DocumentaryUnitDescription.class);
            shouldDelete += Iterables.size(d.getDatePeriods());
            shouldDelete += Iterables.size(d.getAccessPoints());
        }

        String log = "Deleting item";
        Integer deleted = loggingApi(validUser).delete(item.getId(), Optional.of(log));
        assertEquals(Integer.valueOf(shouldDelete), deleted);

        List<SystemEvent> actions = toList(validUser.getActions());

        // Check there's an extra audit log for the user
        assertEquals(origActionCount + 1, actions.size());
        // Check the deletion log has a default label
        // Assumes the action is the last in the list,
        // which it should be as the most recent.
        SystemEvent deleteAction = actions.get(actions.size() - 1);
        assertEquals(log, deleteAction.getLogMessage());
    }
}
