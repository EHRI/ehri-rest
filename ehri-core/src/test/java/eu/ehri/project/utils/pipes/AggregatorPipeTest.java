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