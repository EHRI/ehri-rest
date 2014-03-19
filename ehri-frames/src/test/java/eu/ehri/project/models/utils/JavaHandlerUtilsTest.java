package eu.ehri.project.models.utils;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JavaHandlerUtilsTest {

    private Graph graph;

    @Before
    public void setUp() throws Exception {
        graph = TinkerGraphFactory.createTinkerGraph();
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    @Test
    public void testAddSingleRelationship() throws Exception {
        Vertex v1 = graph.addVertex(null);
        Vertex v2 = graph.addVertex(null);
        Vertex v3 = graph.addVertex(null);
        assertTrue(JavaHandlerUtils.addSingleRelationship(v1, v2, "test"));
        assertFalse(JavaHandlerUtils.addSingleRelationship(v1, v2, "test"));
        assertFalse(JavaHandlerUtils.addSingleRelationship(v1, v1, "test"));

        assertTrue(JavaHandlerUtils.addSingleRelationship(v1, v3, "test"));
        assertTrue(Iterables.contains(v1.getVertices(Direction.OUT), v3));
        assertFalse(Iterables.contains(v1.getVertices(Direction.OUT), v2));

        assertTrue(JavaHandlerUtils.addSingleRelationship(v2, v3, "test"));
        assertTrue(Iterables.contains(v3.getVertices(Direction.IN), v1));
        assertTrue(Iterables.contains(v3.getVertices(Direction.IN), v2));
    }
}
