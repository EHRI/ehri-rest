package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.views.ActionViews;
import eu.ehri.project.test.utils.FixtureLoader;

public class ActionViewsTest extends ViewsTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }    
    
    /**
     * Test updating an item.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     */
    @Test
    public void testUpdate() throws PermissionDenied, ValidationError,
            DeserializationError {
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        DocumentaryUnit unit = docViews.create(bundle, validUserId);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_COLLECTION_NAME + " with new stuff";
        bundle.put("id", unit.asVertex().getId());

        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.put("name", newName);

        DocumentaryUnit changedUnit = docViews.update(bundle, validUserId);
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
     */
    @Test
    public void testUserUpdate() throws PermissionDenied, ValidationError,
            DeserializationError {
        ActionViews<UserProfile> userViews = new ActionViews<UserProfile>(graph,
                UserProfile.class);
        Map<String, Object> bundle = getTestUserBundle();
        UserProfile user = userViews.create(bundle, validUserId);
        assertEquals(TEST_USER_NAME, user.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_USER_NAME + " with new stuff";
        bundle.put("id", user.asVertex().getId());

        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.put("name", newName);

        UserProfile changedUser = userViews.update(bundle, validUserId);
        assertEquals(newName, changedUser.getName());
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @Test
    public void testCreate() throws ValidationError, PermissionDenied,
            DeserializationError {
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        DocumentaryUnit unit = docViews.create(bundle, validUserId);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test we can create a new user.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @Test
    public void testUserCreate() throws ValidationError, PermissionDenied,
            DeserializationError {
        ActionViews<UserProfile> userViews = new ActionViews<UserProfile>(graph,
                UserProfile.class);
        Map<String, Object> bundle = getTestUserBundle();
        UserProfile user = userViews.create(bundle, validUserId);
        assertEquals(TEST_USER_NAME, user.getName());
    }

    /**
     * Test we can create a new group.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @Test
    public void testGroupCreate() throws ValidationError, PermissionDenied,
            DeserializationError {
        ActionViews<Group> groupViews = new ActionViews<Group>(graph, Group.class);
        Map<String, Object> bundle = getTestGroupBundle();
        Group group = groupViews.create(bundle, validUserId);
        assertEquals(TEST_GROUP_NAME, group.getName());
    }

    /**
     * Test creating a view with invalid data throws a validationError
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @Test(expected = ValidationError.class)
    public void testCreateWithError() throws ValidationError, PermissionDenied,
            DeserializationError {
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.remove("name");

        // This should barf because the collection has no name.
        DocumentaryUnit unit = docViews.create(bundle, validUserId);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test creating a view with invalid data throws a validationError
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     */
    @Test(expected = DeserializationError.class)
    public void testCreateWithDeserialisationError() throws ValidationError,
            PermissionDenied, DeserializationError {
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        bundle.remove("data");

        // This should barf because the collection has no name.
        DocumentaryUnit unit = docViews.create(bundle, validUserId);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
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
        ActionViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Integer shouldDelete = 1;
        DocumentaryUnit unit = graph.getVertex(itemId, DocumentaryUnit.class);

        // FIXME: Surely there's a better way of doing this???
        Iterator<DatePeriod> dateIter = unit.getDatePeriods().iterator();
        Iterator<Description> descIter = unit.getDescriptions().iterator();
        for (; dateIter.hasNext(); shouldDelete++)
            dateIter.next();
        for (; descIter.hasNext(); shouldDelete++)
            descIter.next();

        Integer deleted = docViews.delete(itemId, validUserId);
        assertEquals(shouldDelete, deleted);
    }
}
