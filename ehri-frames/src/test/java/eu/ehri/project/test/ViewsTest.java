package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.views.Views;

public class ViewsTest extends ModelTestBase {

    protected static final String TEST_COLLECTION_NAME = "A brand new collection";
    protected static final String TEST_START_DATE = "1945-01-01T00:00:00Z";
    protected static final String TEST_USER_NAME = "Joe Blogs";
    protected static final String TEST_GROUP_NAME = "People";

    // Members closely coupled to the test data!
    protected Long validUserId = 20L;
    protected Long invalidUserId = 21L;
    protected Long itemId = 1L;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Access an item 0 as user 20.
     * 
     * @throws PermissionDenied
     */
    @Test
    public void testDetail() throws PermissionDenied {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        DocumentaryUnit unit = docViews.detail(itemId, validUserId);
        assertEquals(itemId, unit.asVertex().getId());
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
        UserProfile user = userViews.detail(validUserId, validUserId);
        assertEquals(validUserId, user.asVertex().getId());
    }

    /**
     * Access an item as an invalid user. TODO: Check that this test fails if no
     * exception is thrown.
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testDetailPermissionDenied() throws PermissionDenied {
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
                DocumentaryUnit.class);
        docViews.detail(itemId, invalidUserId);
    }

    /**
     * Access the valid user's profile as an invalid users.
     * 
     * @throws PermissionDenied
     */
    @Test(expected = PermissionDenied.class)
    public void testUserDetailPermissionDenied() throws PermissionDenied {
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
                UserProfile.class);
        userViews.detail(validUserId, invalidUserId);
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
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
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
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
        Views<UserProfile> userViews = new Views<UserProfile>(graph,
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
        Views<Group> groupViews = new Views<Group>(graph, Group.class);
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
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
        Views<DocumentaryUnit> docViews = new Views<DocumentaryUnit>(graph,
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

    // Helpers

    // @formatter:off
    @SuppressWarnings("serial")
    protected Map<String, Object> getTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put("id", null);
            put("data", new HashMap<String, Object>() {{
                put("name", TEST_COLLECTION_NAME);
                put("identifier", "someid-01");
                put("isA", EntityTypes.DOCUMENTARY_UNIT);
            }});
            put("relationships", new HashMap<String, Object>() {{
                put("describes", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("id", null);
                        put("data", new HashMap<String, Object>() {{
                            put("identifier", "someid-01");
                            put("title", "A brand new item description");
                            put("isA", EntityTypes.DOCUMENT_DESCRIPTION);
                            put("languageOfDescription", "en");
                        }});
                    }});
                }});
                put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("id", null);
                        put("data", new HashMap<String, Object>() {{
                            put("startDate", TEST_START_DATE);
                            put("endDate", TEST_START_DATE);
                            put("isA", EntityTypes.DATE_PERIOD);
                        }});
                    }});
                }});
            }});
        }};
    }

    @SuppressWarnings("serial")
    protected Map<String, Object> getTestUserBundle() {
        // Data structure representing a not-yet-created user.
        return new HashMap<String, Object>() {{
            put("id", null);
            put("data", new HashMap<String, Object>() {{
                put("name", TEST_USER_NAME);
                put("identifier", "joe-blogs");
                put("userId", 9999L);
                put("isA", EntityTypes.USER_PROFILE);
            }});
        }};
    }

    @SuppressWarnings("serial")
    protected Map<String, Object> getTestGroupBundle() {
        // Data structure representing a not-yet-created group.
        return new HashMap<String, Object>() {{
            put("id", null);
            put("data", new HashMap<String, Object>() {{
                put("name", TEST_GROUP_NAME);
                put("identifier", "people");
                put("isA", EntityTypes.GROUP);
            }});
        }};
    }

    // formatter:on
}
