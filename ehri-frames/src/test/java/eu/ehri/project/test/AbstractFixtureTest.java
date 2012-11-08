package eu.ehri.project.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;

import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

abstract public class AbstractFixtureTest extends ModelTestBase {

    protected static final String TEST_COLLECTION_NAME = "A brand new collection";
    protected static final String TEST_START_DATE = "1945-01-01T00:00:00Z";
    protected static final String TEST_USER_NAME = "Joe Blogs";
    protected static final String TEST_GROUP_NAME = "People";

    // Members closely coupled to the test data!
    protected UserProfile validUser;
    protected UserProfile invalidUser;
    protected DocumentaryUnit item;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();
        item = helper.getTestFrame("c1", DocumentaryUnit.class);
        validUser = helper.getTestFrame("mike", UserProfile.class);
        invalidUser = helper.getTestFrame("reto", UserProfile.class);
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
                put(Accessor.NAME, TEST_COLLECTION_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
                put(EntityType.KEY, EntityTypes.DOCUMENTARY_UNIT);
            }});
            put("relationships", new HashMap<String, Object>() {{
                put("describes", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("id", null);
                        put("data", new HashMap<String, Object>() {{
                            put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
                            put("title", "A brand new item description");
                            put(EntityType.KEY, EntityTypes.DOCUMENT_DESCRIPTION);
                            put("languageCode", "en");
                        }});
                    }});
                }});
                put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("id", null);
                        put("data", new HashMap<String, Object>() {{
                            put(DatePeriod.START_DATE, TEST_START_DATE);
                            put(DatePeriod.END_DATE, TEST_START_DATE);
                            put(EntityType.KEY, EntityTypes.DATE_PERIOD);
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
                put(Accessor.NAME, TEST_USER_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "joe-blogs");
                put(EntityType.KEY, EntityTypes.USER_PROFILE);
            }});
        }};
    }

    @SuppressWarnings("serial")
    protected Map<String, Object> getTestGroupBundle() {
        // Data structure representing a not-yet-created group.
        return new HashMap<String, Object>() {{
            put("id", null);
            put("data", new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_GROUP_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "people");
                put(EntityType.KEY, EntityTypes.GROUP);
            }});
        }};
    }

    // formatter:on
}
