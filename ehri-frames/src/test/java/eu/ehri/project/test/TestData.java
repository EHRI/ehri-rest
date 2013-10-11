package eu.ehri.project.test;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: michaelb
 *
 * Test data structures.
 */
@SuppressWarnings("serial")
public class TestData {
    public static final String TEST_COLLECTION_NAME = "A brand new collection";
    protected static final String TEST_AGENT_NAME = "Test Repo 1";
    public static final String TEST_START_DATE = "1945-01-01T00:00:00Z";
    public static final String TEST_USER_NAME = "Joe Blogs";
    public static final String TEST_GROUP_NAME = "People";

    // @formatter:off
    public static Map<String, Object> getTestDocBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.DOCUMENTARY_UNIT);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Ontology.NAME_KEY, TEST_COLLECTION_NAME);
                put(Ontology.IDENTIFIER_KEY, "someid-01");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put(Ontology.DESCRIPTION_FOR_ENTITY, new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put(Bundle.TYPE_KEY, Entities.DOCUMENT_DESCRIPTION);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(Ontology.IDENTIFIER_KEY, "someid-01");
                            put(Ontology.NAME_KEY, "A brand new item description");
                            put(Ontology.LANGUAGE_OF_DESCRIPTION, "en");
                        }});
                        put(Bundle.REL_KEY, new HashMap<String,Object>() {{
                            put(Ontology.ENTITY_HAS_DATE, new LinkedList<HashMap<String, Object>>() {{
                                add(new HashMap<String, Object>() {{
                                    put(Bundle.TYPE_KEY, Entities.DATE_PERIOD);
                                    put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                                        put(Ontology.DATE_PERIOD_START_DATE, TEST_START_DATE);
                                        put(Ontology.DATE_PERIOD_END_DATE, TEST_START_DATE);
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});
            }});
        }};
    }

    public static Map<String, Object> getTestAgentBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.REPOSITORY);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Ontology.NAME_KEY, TEST_AGENT_NAME);
                put(Ontology.IDENTIFIER_KEY, "test-repo-1");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put(Ontology.DESCRIPTION_FOR_ENTITY, new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put(Bundle.TYPE_KEY, Entities.REPOSITORY_DESCRIPTION);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(Ontology.IDENTIFIER_KEY, "test-repo-1-desc");
                            put(Ontology.NAME_KEY, "A Test Repository");
                            put(Ontology.LANGUAGE_OF_DESCRIPTION, "en");
                        }});
                        put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                            put(Ontology.ENTITY_HAS_ADDRESS, new LinkedList<HashMap<String, Object>>() {{
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

    public static Map<String, Object> getTestUserBundle() {
        // Data structure representing a not-yet-created user.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.USER_PROFILE);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Ontology.NAME_KEY, TEST_USER_NAME);
                put(Ontology.IDENTIFIER_KEY, "joe-blogs");
            }});
        }};
    }

    public static Map<String, Object> getTestGroupBundle() {
        // Data structure representing a not-yet-created group.
        return new HashMap<String, Object>() {{
            put(Bundle.TYPE_KEY, Entities.GROUP);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Ontology.NAME_KEY, TEST_GROUP_NAME);
                put(Ontology.IDENTIFIER_KEY, "people");
            }});
        }};
    }
    // formatter:on
}
