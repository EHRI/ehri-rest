/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.utils.pipes;

import com.google.common.collect.Lists;
import com.tinkerpop.pipes.Pipe;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AggregatorPipeTest {

    @Test
    public void testAggregation() throws Exception {
        Pipe<Integer, List<Integer>> pipe = new AggregatorPipe<>((a, b, count) -> a + 1 == b);

        pipe.setStarts(Arrays.asList(1, 2, 3, 5, 6, 7));
        List<List<Integer>> ranges = Lists.newArrayList(pipe.iterator());
        assertEquals(2, ranges.size());
        assertEquals(3, ranges.get(0).size());
        assertEquals(3, ranges.get(1).size());
    }

    @Test
    public void testAggregationWithCount() throws Exception {
        Pipe<Integer, List<Integer>> pipe = new AggregatorPipe<>((a, b, count) -> count < 2 && a + 1 == b);

        pipe.setStarts(Arrays.asList(1, 2, 3, 5, 6, 7));
        List<List<Integer>> ranges = Lists.newArrayList(pipe.iterator());
        assertEquals(4, ranges.size());
        assertEquals(2, ranges.get(0).size());
        assertEquals(1, ranges.get(1).size());
        assertEquals(2, ranges.get(2).size());
        assertEquals(1, ranges.get(3).size());
    }
}