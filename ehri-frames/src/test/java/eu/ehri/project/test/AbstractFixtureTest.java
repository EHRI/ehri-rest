package eu.ehri.project.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import eu.ehri.project.models.EntityTypes;

abstract public class AbstractFixtureTest extends ModelTestBase {

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

    // Helpers, additional test data

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
