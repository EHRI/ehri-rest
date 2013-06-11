package eu.ehri.project.test;

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import java.util.List;

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
}
