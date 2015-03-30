/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.persistence.utils;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DataUtilsTest {
    @Test
    public void testIsEmptySequence() throws Exception {
        Object t1 = "a string";
        Object t2 = Lists.newArrayList("not empty");
        Object t3 = Lists.newArrayList();
        Object[] t4 = {"not", "empty"};
        Object[] t5 = {};
        Object t6 = Iterables.empty();
        assertFalse(DataUtils.isEmptySequence(t1));
        assertFalse(DataUtils.isEmptySequence(t2));
        assertTrue(DataUtils.isEmptySequence(t3));
        assertFalse(DataUtils.isEmptySequence(t4));
        assertTrue(DataUtils.isEmptySequence(t5));
        assertTrue(DataUtils.isEmptySequence(t6));
    }
}
