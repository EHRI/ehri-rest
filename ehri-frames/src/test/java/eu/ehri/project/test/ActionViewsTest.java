package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;

import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.Serializer;
import org.junit.Test;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.helpers.collection.Iterables;

public class ActionViewsTest extends AbstractFixtureTest {

    /**
     * Test updating an item.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError {
        LoggingCrudViews<DocumentaryUnit> docViews = new LoggingCrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());

        String newName = TEST_COLLECTION_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(manager.getId(unit)).withDataValue(
                "name", newName);

        DocumentaryUnit changedUnit = docViews.update(newBundle, validUser);
        assertEquals(newName, changedUnit.getName());

        // Check the nested item was created correctly
        DatePeriod datePeriod = changedUnit.getDatePeriods().iterator().next();
        assertTrue(datePeriod != null);
        assertEquals(TEST_START_DATE, datePeriod.getStartDate());

        // And that the reverse relationship works.
        assertEquals(changedUnit.asVertex(), datePeriod.getEntity().asVertex());
    }

    /**
     * Test updating a user.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testUserUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError {
        LoggingCrudViews<UserProfile> userViews = new LoggingCrudViews<UserProfile>(
                graph, UserProfile.class);
        Bundle bundle = Bundle.fromData(getTestUserBundle());
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TEST_USER_NAME, user.getName());

        String newName = TEST_USER_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(manager.getId(user)).withDataValue(
                "name", newName);

        UserProfile changedUser = userViews.update(newBundle, validUser);
        assertEquals(newName, changedUser.getName());

        // Check we have an audit action.
        assertNotNull(changedUser.getLatestEvent());
        // FIXME: getLatestAction() should return a single item, but due to
        // a current (2.2.0) limitation in frames' @GremlinGroovy mechanism
        // it can't
        assertEquals(1, Iterables.count(validUser.getLatestAction()));
        SystemEvent event = Iterables.single(validUser.getLatestAction());
        assertTrue(event.getSubjects().iterator()
                .hasNext());
        assertEquals(changedUser.asVertex(), event.getSubjects().iterator().next().asVertex());
        assertTrue(changedUser.getHistory().iterator().hasNext());

        System.out.println("User: " + user.asVertex());
        // We should have exactly two actions now; one for create, one for
        // update...
        List<SystemEvent> events = toList(changedUser.getHistory());
        assertEquals(2, events.size());
        // We should have the right subject on the actionEvent
        assertTrue(events.get(0).getSubjects().iterator().hasNext());
        assertTrue(events.get(1).getSubjects().iterator().hasNext());

        assertEquals(1, Iterables.count(events.get(0).getSubjects()));
        assertEquals(1, Iterables.count(events.get(1).getSubjects()));

        assertEquals(changedUser.asVertex(), events.get(0)
                .getSubjects().iterator().next().asVertex());
        assertEquals(changedUser.asVertex(), events.get(1)
                .getSubjects().iterator().next().asVertex());
        try {
            System.out.println(new Serializer(graph).vertexFrameToBundle(events.get(0)));
        } catch (SerializationError serializationError) {
            serializationError.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        // They should have default log messages, and come out latest-first.
        assertEquals(LoggingCrudViews.DEFAULT_UPDATE_LOG, events.get(0).getLogMessage());
        assertEquals(LoggingCrudViews.DEFAULT_CREATE_LOG, events.get(1).getLogMessage());
    }

    /**
     * Tests that deleting a collection will also delete its dependent
     * relations. NB: This test will break of other @Dependent relations are
     * added to DocumentaryUnit.
     *
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    @Test
    public void testDelete() throws PermissionDenied, ValidationError,
            SerializationError {
        LoggingCrudViews<DocumentaryUnit> docViews = new LoggingCrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Integer shouldDelete = 1;
        int origActionCount = toList(validUser.getHistory()).size();

        // FIXME: Surely there's a better way of doing this???
        Iterator<DatePeriod> dateIter = item.getDatePeriods().iterator();
        Iterator<Description> descIter = item.getDescriptions().iterator();
        for (; dateIter.hasNext(); shouldDelete++)
            dateIter.next();
        for (; descIter.hasNext(); shouldDelete++)
            descIter.next();

        Integer deleted = docViews.delete(item, validUser);
        assertEquals(shouldDelete, deleted);

        List<SystemEvent> actions = toList(validUser.getActions());

        // Check there's an extra audit log for the user
        assertEquals(origActionCount + 1, actions.size());
        // Check the deletion log has a default label
        // Assumes the action is the last in the list,
        // which it should be as the most recent.
        SystemEvent deleteAction = actions.get(actions.size() - 1);
        assertEquals(LoggingCrudViews.DEFAULT_DELETE_LOG,
                deleteAction.getLogMessage());
    }
}
