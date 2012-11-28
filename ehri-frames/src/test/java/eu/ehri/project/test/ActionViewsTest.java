package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.views.ActionViews;

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
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Map<String, Object> testData = getTestBundle();
        DocumentaryUnit unit = docViews.create(testData, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_COLLECTION_NAME + " with new stuff";        

        Map<String, Object> data = (Map<String, Object>) testData.get("data");
        data.put("name", newName);
        data.put(EntityType.ID_KEY, unit.asVertex().getProperty(EntityType.ID_KEY));

        DocumentaryUnit changedUnit = docViews.update(testData, validUser);
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
        ActionViews<UserProfile> userViews = new ActionViews<UserProfile>(
                graph, UserProfile.class);
        Map<String, Object> userData = getTestUserBundle();
        UserProfile user = userViews.create(userData, validUser);
        assertEquals(TEST_USER_NAME, user.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_USER_NAME + " with new stuff";

        Map<String, Object> data = (Map<String, Object>) userData.get("data");
        data.put("name", newName);
        data.put(EntityType.ID_KEY, user.asVertex().getProperty(EntityType.ID_KEY));

        UserProfile changedUser = userViews.update(userData, validUser);
        assertEquals(newName, changedUser.getName());

        // Check we have an audit action.
        assertTrue(changedUser.getHistory().iterator().hasNext());
        // We should have exactly two; one for create, one for update...
        List<Action> actions = toList(changedUser.getHistory());
        assertEquals(2, actions.size());
        // They should have default log messages...
        assertEquals(ActionViews.DEFAULT_CREATE_LOG, actions.get(0)
                .getLogMessage());
        assertEquals(ActionViews.DEFAULT_UPDATE_LOG, actions.get(1)
                .getLogMessage());
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
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Integer shouldDelete = 1;
        int origActionCount = toList(validUser.getActions()).size();

        // FIXME: Surely there's a better way of doing this???
        Iterator<DatePeriod> dateIter = item.getDatePeriods().iterator();
        Iterator<DocumentDescription> descIter = item.getDescriptions().iterator();
        for (; dateIter.hasNext(); shouldDelete++)
            dateIter.next();
        for (; descIter.hasNext(); shouldDelete++)
            descIter.next();

        Integer deleted = docViews.delete(item, validUser);
        assertEquals(shouldDelete, deleted);

        List<Action> actions = toList(validUser.getActions());

        // Check there's an extra audit log for the user
        assertEquals(origActionCount + 1, actions.size());
        // Check the deletion log has a default label
        // Assumes the action is the last in the list,
        // which it should be as the most recent.
        Action deleteAction = actions.get(actions.size() - 1);
        assertEquals(ActionViews.DEFAULT_DELETE_LOG,
                deleteAction.getLogMessage());
    }
}
