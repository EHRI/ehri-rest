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
