package eu.ehri.project.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.test.models.TestFramedInterface;

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
}
