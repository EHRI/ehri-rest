package eu.ehri.project.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import eu.ehri.project.models.base.*;
import eu.ehri.project.persistance.Bundle;
import org.junit.Before;
import org.junit.BeforeClass;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;

abstract public class AbstractFixtureTest extends ModelTestBase {

    protected static final String TEST_COLLECTION_NAME = "A brand new collection";
    protected static final String TEST_AGENT_NAME = "Test Repo 1";
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
        try {
            item = manager.getFrame("c1", DocumentaryUnit.class);
            validUser = manager.getFrame("mike", UserProfile.class);
            invalidUser = manager.getFrame("reto", UserProfile.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(e);
        }
    }

    // Helpers, additional test data

    // @formatter:off
    @SuppressWarnings("serial")
    protected Map<String, Object> getTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.DOCUMENTARY_UNIT);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_COLLECTION_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put(Description.DESCRIBES, new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put(Bundle.TYPE_KEY, Entities.DOCUMENT_DESCRIPTION);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
                            put(Description.NAME, "A brand new item description");
                            put(Description.LANGUAGE_CODE, "en");
                        }});
                        put(Bundle.REL_KEY, new HashMap<String,Object>() {{
                            put(TemporalEntity.HAS_DATE, new LinkedList<HashMap<String, Object>>() {{
                                add(new HashMap<String, Object>() {{
                                    put(Bundle.TYPE_KEY, Entities.DATE_PERIOD);
                                    put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                                        put(DatePeriod.START_DATE, TEST_START_DATE);
                                        put(DatePeriod.END_DATE, TEST_START_DATE);
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});
            }});
        }};
    }

    @SuppressWarnings("serial")
    protected Map<String, Object> getTestAgentBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.AGENT);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_AGENT_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "test-repo-1");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put(Description.DESCRIBES, new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put(Bundle.TYPE_KEY, Entities.AGENT_DESCRIPTION);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(AccessibleEntity.IDENTIFIER_KEY, "test-repo-1-desc");
                            put(Description.NAME, "A Test Repository");
                            put(Description.LANGUAGE_CODE, "en");
                        }});
                        put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                            put(AddressableEntity.HAS_ADDRESS, new LinkedList<HashMap<String, Object>>() {{
                                add(new HashMap<String, Object>() {{
                                    put(Bundle.TYPE_KEY, Entities.ADDRESS);
                                    put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                                        put("name", "primary");
                                    }});
                                }});
                            }});
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
            put(Bundle.TYPE_KEY, Entities.USER_PROFILE);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_USER_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "joe-blogs");
            }});
        }};
    }

    @SuppressWarnings("serial")
    protected Map<String, Object> getTestGroupBundle() {
        // Data structure representing a not-yet-created group.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.GROUP);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_GROUP_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "people");
            }});
        }};
    }

    // formatter:on
}
