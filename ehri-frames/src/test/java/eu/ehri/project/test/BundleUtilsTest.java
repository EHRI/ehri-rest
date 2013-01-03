package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.utils.BundleUtils;

public class BundleUtilsTest extends AbstractFixtureTest {

    @Test
    public void testGetPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals(TEST_COLLECTION_NAME,
                BundleUtils.get(bundle, Accessor.NAME));
        assertEquals(TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        assertEquals(TEST_START_DATE, BundleUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        assertEquals("en", BundleUtils.get(bundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testGetPathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals("en",
                BundleUtils.get(bundle, "idontexist[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundleIndexError.class)
    public void testGetPathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals("en", BundleUtils.get(bundle, "describes[2]/languageCode"));
    }

    @Test
    public void testUpdatePath() throws Exception {
        String testDate = "2012-12-12";
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle newBundle = BundleUtils.set(bundle, "describes[0]/languageCode",
                "fr");
        assertEquals("fr",
                BundleUtils.get(newBundle, "describes[0]/languageCode"));
        newBundle = BundleUtils.set(bundle,
                "describes[0]/hasDate[0]/startDate", testDate);
        assertEquals(testDate,
                BundleUtils.get(newBundle, "describes[0]/hasDate[0]/startDate"));
        assertEquals(TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        newBundle = BundleUtils.set(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate", testDate);
        assertEquals(testDate, BundleUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        assertEquals(TEST_START_DATE, BundleUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
    }

    @Test
    public void testSetNestedNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = BundleUtils.getBundle(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = BundleUtils.setBundle(bundle,
                "describes[0]/hasDate[0]/hasDate[0]", dateBundle);
        assertEquals("testval", BundleUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/testattr"));
    }

    @Test
    public void testDeletePath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals("en", BundleUtils.get(bundle, "describes[0]/languageCode"));
        Bundle newBundle = BundleUtils.delete(bundle,
                "describes[0]/languageCode");
        assertNull(BundleUtils.get(newBundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testDeleteNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals(TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        Bundle newBundle = BundleUtils.deleteBundle(bundle,
                "describes[0]/hasDate[0]");
        assertEquals(TEST_START_DATE,
                BundleUtils.get(newBundle, "describes[0]/hasDate[0]/startDate"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testUpdatePathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        BundleUtils.set(bundle, "idontexist[0]/languageCode", "fr");
    }

    @Test(expected = BundleUtils.BundleIndexError.class)
    public void testUpdatePathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        BundleUtils.set(bundle, "describes[2]/languageCode", "fr");
    }

    // Helpers

    // @formatter:off
    @SuppressWarnings("serial")
    @Override
    protected Map<String, Object> getTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put("type", Entities.DOCUMENTARY_UNIT);
            put("data", new HashMap<String, Object>() {{
                put(Accessor.NAME, TEST_COLLECTION_NAME);
                put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
            }});
            put("relationships", new HashMap<String, Object>() {{
                put("describes", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("type", Entities.DOCUMENT_DESCRIPTION);
                        put("data", new HashMap<String, Object>() {{
                            put(AccessibleEntity.IDENTIFIER_KEY, "someid-01");
                            put("title", "A brand new item description");
                            put("languageCode", "en");
                        }});
                        put("relationships", new HashMap<String, Object>() {{
                            put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                                add(new HashMap<String, Object>() {{
                                    put("type", Entities.DATE_PERIOD);
                                    put("data", new HashMap<String, Object>() {{
                                        put(DatePeriod.START_DATE, TEST_START_DATE);
                                        put(DatePeriod.END_DATE, TEST_START_DATE);
                                    }});
                                    put("relationships", new HashMap<String, Object>() {{
                                        put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                                            add(new HashMap<String, Object>() {{
                                                put("type", Entities.DATE_PERIOD);
                                                put("data", new HashMap<String, Object>() {{
                                                    put(DatePeriod.START_DATE, TEST_START_DATE);
                                                    put(DatePeriod.END_DATE, TEST_START_DATE);
                                                }});
                                            }});
                                        }});
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});
                put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("type", Entities.DATE_PERIOD);
                        put("data", new HashMap<String, Object>() {{
                            put(DatePeriod.START_DATE, TEST_START_DATE);
                            put(DatePeriod.END_DATE, TEST_START_DATE);
                        }});
                    }});
                }});
            }});
        }};
    }
    
    // @formatter:on
}
