package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import eu.ehri.project.models.*;
import org.junit.Test;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.CrudViews;
import eu.ehri.project.views.impl.LoggingCrudViews;

public class ViewsTest extends AbstractFixtureTest {

    /**
     * Access an item 0 as user 20.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testDetail() throws PermissionDenied {
        Crud<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(graph,
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
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        docViews.detail(item, AnonymousAccessor.getInstance());
    }

    /**
     * Access an item as an invalid user.
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testDetailPermissionDenied() throws PermissionDenied {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        docViews.detail(item, invalidUser);
    }

    /**
     * Access the valid user's profile as an invalid users.
     * 
     * TODO: Check write access!!!
     */
    public void testUserDetailPermissionDenied() throws PermissionDenied {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
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
     * @throws ItemNotFound
     */
    @Test
    public void testUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TEST_COLLECTION_NAME, unit.getName());

        String newName = TEST_COLLECTION_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(manager.getId(unit)).withDataValue(
                "name", newName);

        DocumentaryUnit changedUnit = docViews.update(newBundle, validUser);
        assertEquals(newName, changedUnit.getName());
        DocumentDescription desc = graph.frame(
                changedUnit.getDescriptions().iterator().next().asVertex(),
                DocumentDescription.class);

        // Check the nested item was created correctly
        DatePeriod datePeriod = desc.getDatePeriods().iterator().next();
        assertTrue(datePeriod != null);
        assertEquals(TEST_START_DATE, datePeriod.getStartDate());

        // And that the reverse relationship works.
        assertEquals(desc.asVertex(), datePeriod.getEntity().asVertex());
    }

    /**
     * Test updating a user.
     * 
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    @Test
    public void testUserUpdate() throws PermissionDenied, ValidationError,
            DeserializationError, IntegrityError, ItemNotFound {
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        Bundle bundle = Bundle.fromData(getTestUserBundle());
        UserProfile user = userViews.create(bundle, validUser);
        assertEquals(TEST_USER_NAME, user.getName());

        String newName = TEST_USER_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(manager.getId(user)).withDataValue(
                "name", newName);
        UserProfile changedUser = userViews.update(newBundle, validUser);
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle());
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle());
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
     * @throws ItemNotFound
     */
    @Test
    public void testCreateAsUnauthorizedAndThenGrant() throws ValidationError,
            PermissionDenied, DeserializationError, IntegrityError,
            ItemNotFound {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle());

        try {
            docViews.create(bundle, invalidUser);
            fail("Creation should throw "
                    + PermissionDenied.class.getSimpleName());
        } catch (PermissionDenied e) {
            // We expected that permission denied... now explicitely add
            // permissions.
            PermissionGrantTarget target = manager.getFrame(
                    ContentTypes.DOCUMENTARY_UNIT.getName(),
                    PermissionGrantTarget.class);
            new AclManager(graph).grantPermissions(invalidUser, target,
                    PermissionType.CREATE);
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
     * @throws ItemNotFound
     */
    @Test
    public void testCreateWithScope() throws ValidationError, PermissionDenied,
            DeserializationError, IntegrityError, ItemNotFound {
        Crud<DocumentaryUnit> docViews = new LoggingCrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class, manager.getFrame("r1",
                        Agent.class));
        Bundle bundle = Bundle.fromData(getTestBundle());
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
        CrudViews<UserProfile> userViews = new CrudViews<UserProfile>(graph,
                UserProfile.class);
        Bundle bundle = Bundle.fromData(getTestUserBundle());
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
        CrudViews<Group> groupViews = new CrudViews<Group>(graph, Group.class);
        Bundle bundle = Bundle.fromData(getTestGroupBundle());
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(getTestBundle())
                .removeDataValue("name");

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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        Integer shouldDelete = 1;

        // FIXME: Surely there's a better way of doing this???
        Iterator<Description> descIter = item.getDescriptions().iterator();
        for (; descIter.hasNext(); shouldDelete++) {
            DocumentDescription d = graph.frame(descIter.next().asVertex(), DocumentDescription.class);
            for (DatePeriod dp : d.getDatePeriods()) shouldDelete++;
        }

        Integer deleted = docViews.delete(item, validUser);
        assertEquals(shouldDelete, deleted);
    }
}
