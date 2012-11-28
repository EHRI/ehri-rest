package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.views.ActionViews;
import eu.ehri.project.views.IViews;
import eu.ehri.project.views.Views;

public class ViewsTest extends AbstractFixtureTest {

    /**
     * Access an item 0 as user 20.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testDetail() throws PermissionDenied {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        DocumentaryUnit unit = docViews.detail(item, validUser);
        assertEquals(item.asVertex(), unit.asVertex());
    }

    /**
     * Check we can access the user's own profile.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testUserProfile() throws PermissionDenied {
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
                UserProfile.class);
        UserProfile user = userViews.detail(validUser, validUser);
        assertEquals(validUser.asVertex(), user.asVertex());
    }

    /**
     * Access an item as an anon user. This should throw PermissionDenied
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testDetailAnonymous() throws PermissionDenied {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        docViews.detail(item, new AnonymousAccessor());
    }

    /**
     * Access an item as an invalid user.
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testDetailPermissionDenied() throws PermissionDenied {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        docViews.detail(item, invalidUser);
    }

    /**
     * Access the valid user's profile as an invalid users.
     * 
     * TODO: Check write access!!!
     */
    public void testUserDetailPermissionDenied() throws PermissionDenied {
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
                UserProfile.class);
        userViews.detail(validUser, invalidUser);
    }

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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_COLLECTION_NAME + " with new stuff";
        bundle.put("id", unit.asVertex().getId());

        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.put("name", newName);

        DocumentaryUnit changedUnit = docViews.update(bundle, validUser);
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
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
                UserProfile.class);
        Map<String, Object> bundle = getTestUserBundle();
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TEST_USER_NAME, user.getName());

        // We could convert the FramedNode back into a bundle here,
        // but let's instead just modify the initial data.
        String newName = TEST_USER_NAME + " with new stuff";
        bundle.put("id", user.asVertex().getId());

        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.put("name", newName);

        UserProfile changedUser = userViews.update(bundle, validUser);
        assertEquals(newName, changedUser.getName());
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test(expected = PermissionDenied.class)
    public void testCreateAsUnauthorized() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        DocumentaryUnit unit = docViews.create(bundle, invalidUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testCreateAsUnauthorizedAndThenGrant() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();

        try {
            docViews.create(bundle, invalidUser);
            fail("Creation should throw "
                    + PermissionDenied.class.getSimpleName());
        } catch (PermissionDenied e) {
            // We expected that permission denied... now explicitely add
            // permissions.
            PermissionGrantTarget target = helper.getTestFrame(
                    "documentaryUnitType", PermissionGrantTarget.class);
            Permission perm = helper.getTestFrame("permCreate",
                    Permission.class);
            new AclManager(graph).grantPermissions(invalidUser, target, perm);
            DocumentaryUnit unit = docViews.create(bundle, invalidUser);
            assertEquals(TEST_COLLECTION_NAME, unit.getName());
        }
    }

    /**
     * Test we can create a node and its subordinates from a set of data.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testCreateWithScope() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        IViews<DocumentaryUnit> docViews = new ActionViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class, helper.getTestFrame("r1", Agent.class));
        Map<String, Object> bundle = getTestBundle();
        // In the fixtures, 'reto' should have a grant for 'CREATE'
        // scoped to the 'r1' repository.
        DocumentaryUnit unit = docViews.create(bundle, invalidUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test we can create a new user.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testUserCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
                UserProfile.class);
        Map<String, Object> bundle = getTestUserBundle();
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TEST_USER_NAME, user.getName());
    }

    /**
     * Test we can create a new group.
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test
    public void testGroupCreate() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        Views<Group> groupViews = new Views<Group>(graph, Group.class);
        Map<String, Object> bundle = getTestGroupBundle();
        Group group = groupViews.create(bundle, validUser);
        assertEquals(TEST_GROUP_NAME, group.getName());
    }

    /**
     * Test creating a view with invalid data throws a validationError
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test(expected = ValidationError.class)
    public void testCreateWithError() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        Map<String, Object> data = (Map<String, Object>) bundle.get("data");
        data.remove("name");

        // This should barf because the collection has no name.
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());
    }

    /**
     * Test creating a view with invalid data throws a validationError
     * 
     * @throws ValidationError
     * @throws PermissionDenied
     * @throws DeserializationError
     * @throws IntegrityError
     */
    @Test(expected = DeserializationError.class)
    public void testCreateWithDeserialisationError() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Map<String, Object> bundle = getTestBundle();
        bundle.remove("data");

        // This should barf because the collection has no name.
        DocumentaryUnit unit = docViews.create(bundle, validUser);
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        Integer shouldDelete = 1;

        // FIXME: Surely there's a better way of doing this???
        Iterator<DatePeriod> dateIter = item.getDatePeriods().iterator();
        Iterator<DocumentDescription> descIter = item.getDescriptions().iterator();
        for (; dateIter.hasNext(); shouldDelete++)
            dateIter.next();
        for (; descIter.hasNext(); shouldDelete++)
            descIter.next();

        Integer deleted = docViews.delete(item, validUser);
        assertEquals(shouldDelete, deleted);
    }
}
