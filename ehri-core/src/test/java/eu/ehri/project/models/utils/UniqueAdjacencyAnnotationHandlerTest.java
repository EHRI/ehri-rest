package eu.ehri.project.models.utils;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;
import eu.ehri.project.models.annotations.UniqueAdjacency;
import eu.ehri.project.test.GraphTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class UniqueAdjacencyAnnotationHandlerTest extends GraphTestBase {

    private FramedGraph<?> graph;
    private TestFrame f1;
    private TestFrame f2;
    private TestFrame f3;

    private static final String UNIQUE = "uniqueRel";
    private static final String SINGLE = "singleRel";
    private static final String NORMAL = "normalRel";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        graph = getFramedGraph();
        f1 = graph.frame(graph.addVertex(1), TestFrame.class);
        f2 = graph.frame(graph.addVertex(2), TestFrame.class);
        f3 = graph.frame(graph.addVertex(3), TestFrame.class);
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    public interface TestFrame extends VertexFrame {

        @UniqueAdjacency(label = NORMAL)
        int countNormal();

        @Adjacency(label = NORMAL)
        void addNormal(TestFrame other);

        @UniqueAdjacency(label = UNIQUE)
        TestFrame getUnique();

        @UniqueAdjacency(label = UNIQUE)
        boolean isUnqiue();

        @UniqueAdjacency(label = UNIQUE)
        void addUnique(TestFrame other);

        @UniqueAdjacency(label = UNIQUE)
        void setUnique(TestFrame other);

        @UniqueAdjacency(label = SINGLE, single = true)
        TestFrame getSingle();

        @UniqueAdjacency(label = SINGLE, single = true)
        boolean isSingle();

        @UniqueAdjacency(label = SINGLE, single = true)
        void addSingle(TestFrame other);

        @UniqueAdjacency(label = SINGLE, single = true)
        void setSingle(TestFrame other);
    }

    @Test
    public void testUniqueAdjacencyIsUnique() throws Exception {
        assertFalse(f1.isUnqiue());
        f1.addUnique(f2);
        f1.addUnique(f2);
        assertEquals(1, Iterables.size(
                f1.asVertex().getEdges(Direction.OUT, UNIQUE)));
        f1.addUnique(f3);
        assertEquals(2, Iterables.size(
                f1.asVertex().getEdges(Direction.OUT, UNIQUE)));
        assertTrue(f1.isUnqiue());
    }

    @Test
    public void testUniqueAdjacencySetNull() throws Exception {
        f1.setUnique(f2);
        assertEquals(f2, f1.getUnique());
        f1.setUnique(null);
        assertNull(f1.getUnique());
    }

    @Test
    public void testUniqueAdjacencyIsSingle() throws Exception {
        assertFalse(f1.isSingle());
        f1.addSingle(f2);
        f1.addSingle(f2);
        f1.addSingle(f3);
        assertEquals(0, Iterables.size(
                f2.asVertex().getEdges(Direction.IN, SINGLE)));
        assertFalse(f1.isUnqiue());
    }

    @Test
    public void testCounting() throws Exception {
        f1.addNormal(f2);
        f1.addNormal(f2);
        f1.addNormal(f3);
        assertEquals(3, f1.countNormal());
        assertEquals(3, Iterables.size(
                f1.asVertex().getEdges(Direction.OUT, NORMAL)));
    }
}