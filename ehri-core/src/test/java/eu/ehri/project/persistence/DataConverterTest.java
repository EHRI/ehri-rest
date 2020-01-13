/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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

package eu.ehri.project.persistence;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static eu.ehri.project.persistence.DataConverter.isEmptySequence;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for Bundle data conversion functions.
 */
public class DataConverterTest extends AbstractFixtureTest {

    @Test
    public void testBundleStream() throws Exception {
        try (InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream("bundle-list.json");
             CloseableIterable<Bundle> bundleMappingIterator = DataConverter.bundleStream(stream)) {
            List<String> ids = Lists.newArrayList();
            for (Bundle bundle : bundleMappingIterator) {
                ids.add(bundle.getId());
            }
            assertEquals(Lists.newArrayList("item1", "item2"), ids);
        }
    }

    @Test
    public void testBundleStreamWithEmptyList() throws Exception {
        InputStream stream = new ByteArrayInputStream("[]".getBytes());
        try (CloseableIterable<Bundle> bundleMappingIterator = DataConverter.bundleStream(stream)) {
            List<Bundle> ids = Lists.newArrayList(bundleMappingIterator);
            assertTrue(ids.isEmpty());
        }
    }

    @Test
    public void testIsEmptySequence() throws Exception {
        Object t1 = "a string";
        Object t2 = Lists.newArrayList("not empty");
        Object t3 = Lists.newArrayList();
        Object[] t4 = {"not", "empty"};
        Object[] t5 = {};
        Object t6 = (Iterable) () -> Lists.newArrayList().iterator();
        assertFalse(isEmptySequence(t1));
        assertFalse(isEmptySequence(t2));
        assertTrue(isEmptySequence(t3));
        assertFalse(isEmptySequence(t4));
        assertTrue(isEmptySequence(t5));
        assertTrue(isEmptySequence(t6));
    }
}
