package eu.ehri.project.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

public class FramesTest extends ModelTestBase {

    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testFramesWithArrayProperty() {
        Vertex v = graph.getBaseGraph().addVertex(null);
        TestFramedInterface test = graph.frame(v, TestFramedInterface.class);
        String[] testData = { "foo", "bar" };
        test.setArray(testData);
        assertArrayEquals("arrays are not equal", testData, test.getArray());
        
        String[] badTestData = {};
        test.setArray(badTestData);
        System.out.println(test.getArray()[0]);
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
