package eu.ehri.project.persistence.utils;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class BundleUtilsTest extends AbstractFixtureTest {

    @Test
    public void testGetPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_COLLECTION_NAME,
                BundleUtils.get(bundle, Ontology.NAME_KEY));
        Assert.assertEquals(TestData.TEST_START_DATE,
                BundleUtils.getBundle(bundle, "describes[0]/hasDate[0]")
                    .getDataValue(Ontology.DATE_PERIOD_START_DATE));
    }

    @Test
    public void testGetBundle() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_COLLECTION_NAME,
                BundleUtils.get(bundle, Ontology.NAME_KEY));
        Assert.assertEquals(TestData.TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE, BundleUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        Assert.assertEquals("en", BundleUtils.get(bundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testGetPathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en",
                BundleUtils.get(bundle, "idontexist[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundleIndexError.class)
    public void testGetPathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en", BundleUtils.get(bundle, "describes[2]/languageCode"));
    }

    @Test
    public void testUpdatePath() throws Exception {
        String testDate = "2012-12-12";
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle newBundle = BundleUtils.set(bundle, "describes[0]/languageCode",
                "fr");
        Assert.assertEquals("fr",
                BundleUtils.get(newBundle, "describes[0]/languageCode"));
        newBundle = BundleUtils.set(bundle,
                "describes[0]/hasDate[0]/startDate", testDate);
        Assert.assertEquals(testDate,
                BundleUtils.get(newBundle, "describes[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        newBundle = BundleUtils.set(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate", testDate);
        Assert.assertEquals(testDate, BundleUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE, BundleUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
    }

    @Test
    public void testSetNestedNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = BundleUtils.getBundle(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = BundleUtils.setBundle(bundle,
                "describes[0]/hasDate[0]/hasDate[0]", dateBundle);
        Assert.assertEquals("testval", BundleUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/testattr"));
    }

    @Test
    public void testInsertNestedNode() throws Exception {
        // Using -1 for insert means to append at the end of
        // the current relationship set...
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = BundleUtils.getBundle(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = BundleUtils.setBundle(bundle,
                "describes[0]/hasDate[0]/hasDate[-1]", dateBundle);
        Assert.assertEquals("testval", BundleUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[1]/testattr"));
    }

    @Test
    public void testDeletePath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en", BundleUtils.get(bundle, "describes[0]/languageCode"));
        Bundle newBundle = BundleUtils.delete(bundle,
                "describes[0]/languageCode");
        Assert.assertNull(BundleUtils.get(newBundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testDeleteNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_START_DATE,
                BundleUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        Bundle newBundle = BundleUtils.deleteBundle(bundle,
                "describes[0]/hasDate[0]");
        Assert.assertEquals(TestData.TEST_START_DATE,
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
    private Map<String, Object> getTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put("type", Entities.DOCUMENTARY_UNIT);
            put("data", new HashMap<String, Object>() {{
                put(Ontology.NAME_KEY, TestData.TEST_COLLECTION_NAME);
                put(Ontology.IDENTIFIER_KEY, "someid-01");
            }});
            put("relationships", new HashMap<String, Object>() {{
                put("describes", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put("type", Entities.DOCUMENT_DESCRIPTION);
                        put("data", new HashMap<String, Object>() {{
                            put(Ontology.IDENTIFIER_KEY, "someid-01");
                            put("title", "A brand new item description");
                            put("languageCode", "en");
                        }});
                        put("relationships", new HashMap<String, Object>() {{
                            put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                                add(new HashMap<String, Object>() {{
                                    put("type", Entities.DATE_PERIOD);
                                    put("data", new HashMap<String, Object>() {{
                                        put(Ontology.DATE_PERIOD_START_DATE, TestData.TEST_START_DATE);
                                        put(Ontology.DATE_PERIOD_END_DATE, TestData.TEST_START_DATE);
                                    }});
                                    put("relationships", new HashMap<String, Object>() {{
                                        put("hasDate", new LinkedList<HashMap<String, Object>>() {{
                                            add(new HashMap<String, Object>() {{
                                                put("type", Entities.DATE_PERIOD);
                                                put("data", new HashMap<String, Object>() {{
                                                    put(Ontology.DATE_PERIOD_START_DATE, TestData.TEST_START_DATE);
                                                    put(Ontology.DATE_PERIOD_END_DATE, TestData.TEST_START_DATE);
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
                            put(Ontology.DATE_PERIOD_START_DATE, TestData.TEST_START_DATE);
                            put(Ontology.DATE_PERIOD_END_DATE, TestData.TEST_START_DATE);
                        }});
                    }});
                }});
            }});
        }};
    }
    
    // @formatter:on
}
