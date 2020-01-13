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

package eu.ehri.project.test;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FramesTest extends ModelTestBase {

    @Test(expected=IllegalArgumentException.class)
    public void testFramesWithListProperty() {
        Vertex v = graph.getBaseGraph().addVertex(null);
        TestFramedInterface test = graph.frame(v, TestFramedInterface.class);
        List<String> testData = Lists.newArrayList("foo", "bar");
        test.setList(testData);
        assertEquals("lists are not equal", testData, test.getList());
        
        List<String> badTestData = Lists.newArrayList();
        // Setting an empty list should barf...
        test.setList(badTestData);
    }

    @Test
    public void testFramesComparison() {
        Vertex v = graph.getBaseGraph().addVertex(null);
        TestFramedInterface f1 = graph.frame(v, TestFramedInterface.class);
        TestFramedInterface2 f2 = graph.frame(v, TestFramedInterface2.class);

        // These should be equal because they frame the same vertex, despite
        // being different interfaces.
        assertEquals(f1, f2);
        assertTrue(f1.equals(f2));
    }
}
