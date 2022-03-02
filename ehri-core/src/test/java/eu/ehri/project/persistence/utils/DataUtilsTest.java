/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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

public class DataUtilsTest extends AbstractFixtureTest {

    @Test
    public void testGetPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_COLLECTION_NAME,
                DataUtils.get(bundle, Ontology.NAME_KEY));
        Assert.assertEquals(TestData.TEST_START_DATE,
                DataUtils.getItem(bundle, "describes[0]/hasDate[0]")
                    .getDataValue(Ontology.DATE_PERIOD_START_DATE));
    }

    @Test
    public void testGetBundle() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_COLLECTION_NAME,
                DataUtils.get(bundle, Ontology.NAME_KEY));
        Assert.assertEquals(TestData.TEST_START_DATE,
                DataUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE, DataUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        Assert.assertEquals("en", DataUtils.get(bundle, "describes[0]/languageCode"));
    }

    @Test(expected = DataUtils.BundlePathError.class)
    public void testGetPathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en",
                DataUtils.get(bundle, "idontexist[0]/languageCode"));
    }

    @Test(expected = DataUtils.BundleIndexError.class)
    public void testGetPathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en", DataUtils.get(bundle, "describes[2]/languageCode"));
    }

    @Test
    public void testUpdatePath() throws Exception {
        String testDate = "2012-12-12";
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle newBundle = DataUtils.set(bundle, "describes[0]/languageCode",
                "fr");
        Assert.assertEquals("fr",
                DataUtils.get(newBundle, "describes[0]/languageCode"));
        newBundle = DataUtils.set(bundle,
                "describes[0]/hasDate[0]/startDate", testDate);
        Assert.assertEquals(testDate,
                DataUtils.get(newBundle, "describes[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE,
                DataUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        newBundle = DataUtils.set(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate", testDate);
        Assert.assertEquals(testDate, DataUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
        Assert.assertEquals(TestData.TEST_START_DATE, DataUtils.get(bundle,
                "describes[0]/hasDate[0]/hasDate[0]/startDate"));
    }

    @Test
    public void testSetNestedNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = DataUtils.getItem(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = DataUtils.setItem(bundle,
                "describes[0]/hasDate[0]/hasDate[0]", dateBundle);
        Assert.assertEquals("testval", DataUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[0]/testattr"));
    }

    @Test
    public void testSetNestedNodeAtNewPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = DataUtils.getItem(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = DataUtils.setItem(bundle,
                "describes[0]/hasDate[0]/newPath[-1]", dateBundle);
        Assert.assertEquals("testval", DataUtils.get(newBundle,
                "describes[0]/hasDate[0]/newPath[0]/testattr"));
    }

    @Test
    public void testInsertNestedNode() throws Exception {
        // Using -1 for insert means to append at the end of
        // the current relationship set...
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle dateBundle = DataUtils.getItem(bundle,
                "describes[0]/hasDate[0]").withDataValue("testattr", "testval");
        Bundle newBundle = DataUtils.setItem(bundle,
                "describes[0]/hasDate[0]/hasDate[-1]", dateBundle);
        Assert.assertEquals("testval", DataUtils.get(newBundle,
                "describes[0]/hasDate[0]/hasDate[1]/testattr"));
    }

    @Test
    public void testDeletePath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals("en", DataUtils.get(bundle, "describes[0]/languageCode"));
        Bundle newBundle = DataUtils.delete(bundle,
                "describes[0]/languageCode");
        Assert.assertNull(DataUtils.get(newBundle, "describes[0]/languageCode"));
    }

    @Test(expected = DataUtils.BundlePathError.class)
    public void testDeleteNode() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Assert.assertEquals(TestData.TEST_START_DATE,
                DataUtils.get(bundle, "describes[0]/hasDate[0]/startDate"));
        Bundle newBundle = DataUtils.deleteItem(bundle,
                "describes[0]/hasDate[0]");
        Assert.assertEquals(TestData.TEST_START_DATE,
                DataUtils.get(newBundle, "describes[0]/hasDate[0]/startDate"));
    }

    @Test(expected = DataUtils.BundlePathError.class)
    public void testUpdatePathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        DataUtils.set(bundle, "idontexist[0]/languageCode", "fr");
    }

    @Test(expected = DataUtils.BundleIndexError.class)
    public void testUpdatePathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        DataUtils.set(bundle, "describes[2]/languageCode", "fr");
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
                        put("type", Entities.DOCUMENTARY_UNIT_DESCRIPTION);
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
