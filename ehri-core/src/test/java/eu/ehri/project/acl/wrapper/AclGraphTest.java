package eu.ehri.project.acl.wrapper;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for ACL graph that provides a 'view' of the available
 * nodes that a given user is allowed to see. In these tests
 * the vertex representing documentary unit 'c1' has visibility
 * restrictions preventing it being seen by the invalid user.
 */
public class AclGraphTest extends AbstractFixtureTest {

    public AclGraph<?> validUserGraph;
    public AclGraph<?> invalidUserGraph;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        super.setUp();
        validUserGraph = new AclGraph(graph.getBaseGraph(), validUser);
        invalidUserGraph = new AclGraph(graph.getBaseGraph(), invalidUser);
    }

    @Test
    public void testGetVertexAsValidUser() throws Exception {
        Vertex vertex = manager.getEntity("c1", DocumentaryUnit.class).asVertex();
        Vertex validUserVertex = validUserGraph.getVertex(vertex.getId());
        assertEquals(validUserVertex.getId(), vertex.getId());
    }

    @Test
    public void testGetVertexAsInvalidUser() throws Exception {
        Vertex vertex = manager.getEntity("c1", DocumentaryUnit.class).asVertex();
        Vertex invalidUserVertex = invalidUserGraph.getVertex(vertex.getId());
        assertNull(invalidUserVertex);
    }

    @Test
    public void testGetVerticesAsValidUser() throws Exception {
        Iterable<Vertex> vertices = validUserGraph.getVertices(Ontology.IDENTIFIER_KEY, "c1");
        assertTrue(vertices.iterator().hasNext());
        assertEquals(1, Iterables.size(vertices));
    }

    @Test
    public void testGetVerticesAsInvalidUser() throws Exception {
        Iterable<Vertex> vertices = invalidUserGraph.getVertices(Ontology.IDENTIFIER_KEY, "c1");
        assertFalse(vertices.iterator().hasNext());
        assertEquals(0, Iterables.size(vertices));
    }

    @Test
    public void testGetEdgesAttachedToInvisibleNodes() throws Exception {
        List<Edge> edges1 = Lists.newArrayList(validUserGraph.getEdges());
        List<Edge> edges2 = Lists.newArrayList(invalidUserGraph.getEdges());
        assertEquals(edges2.size() + 39, edges1.size());
    }

    @Test
    public void testTraversalAsInvalidUser() throws Exception {
        Vertex vertex = manager.getEntity("r1", Repository.class).asVertex();
        Vertex invalidUserVertex = invalidUserGraph.getVertex(vertex.getId());
        // Invalid user can see repository r1
        assertNotNull(invalidUserVertex);
        // Because three of r1's doc unit nodes are restricted visibility, we
        // should only get two nodes (c4 and nl-r1-m19) when we traverse 'heldBy'
        Iterable<Vertex> docs = invalidUserVertex
                .getVertices(Direction.IN, Ontology.DOC_HELD_BY_REPOSITORY);
        assertEquals(2, Iterables.size(docs));
    }

    @Test
    public void testDumpAndLoad() throws Exception {
        int vCount = Iterators.size(invalidUserGraph.getVertices().iterator());
        int eCount = Iterators.size(invalidUserGraph.getEdges().iterator());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GraphSONWriter.outputGraph(invalidUserGraph, baos, GraphSONMode.EXTENDED);

        resetGraph();
        FramedGraph<? extends TransactionalGraph> newGraph = getFramedGraph();
        ByteArrayInputStream ios = new ByteArrayInputStream(baos.toByteArray());
        GraphSONReader.inputGraph(newGraph, ios);
        newGraph.getBaseGraph().commit();
        assertEquals(vCount, Iterators.size(newGraph.getVertices().iterator()));
        assertEquals(eCount, Iterators.size(newGraph.getEdges().iterator()));
    }
}