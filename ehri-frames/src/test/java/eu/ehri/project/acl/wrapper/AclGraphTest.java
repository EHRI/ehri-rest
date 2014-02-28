package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;

/**
 * Tests for ACL graph that provides a 'view' of the available
 * nodes that a given user is allowed to see. In these tests
 * the vertex representing documentary unit 'c1' has visibility
 * restrictions preventing it being seen by the invalid user.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclGraphTest extends AbstractFixtureTest {

    public AclGraph<?> validUserGraph;
    public AclGraph<?> invalidUserGraph;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        super.setUp();
        validUserGraph = new AclGraph(graph.getBaseGraph(), new AclGraph.AccessorFetcher() {
            @Override
            public Accessor fetch() {
                return validUser;
            }
        });
        invalidUserGraph =  new AclGraph(graph.getBaseGraph(), new AclGraph.AccessorFetcher() {
            @Override
            public Accessor fetch() {
                return invalidUser;
            }
        });
    }

    @Test
    public void testGetVertexAsValidUser() throws Exception {
        Vertex vertex = manager.getFrame("c1", DocumentaryUnit.class).asVertex();
        Vertex validUserVertex = validUserGraph.getVertex(vertex.getId());
        assertEquals(validUserVertex.getId(), vertex.getId());
    }

    @Test
    public void testGetVertexAsInvalidUser() throws Exception {
        Vertex vertex = manager.getFrame("c1", DocumentaryUnit.class).asVertex();
        Vertex invalidUserVertex = invalidUserGraph.getVertex(vertex.getId());
        assertNull(invalidUserVertex);
    }

    @Test
    public void testGetVerticesAsValidUser() throws Exception {
        Iterable<Vertex> vertices = validUserGraph.getVertices(Ontology.IDENTIFIER_KEY, "c1");
        assertTrue(vertices.iterator().hasNext());
        assertEquals(1L, Iterables.count(vertices));
    }

    @Test
    public void testGetVerticesAsInvalidUser() throws Exception {
        Iterable<Vertex> vertices = invalidUserGraph.getVertices(Ontology.IDENTIFIER_KEY, "c1");
        assertFalse(vertices.iterator().hasNext());
        assertEquals(0L, Iterables.count(vertices));
    }

    @Test
    public void testTraversalAsInvalidUser() throws Exception {
        Vertex vertex = manager.getFrame("r1", Repository.class).asVertex();
        Vertex invalidUserVertex = invalidUserGraph.getVertex(vertex.getId());
        // Invalid user can see repository r1
        assertNotNull(invalidUserVertex);
        // Because three of r1's doc unit nodes are restricted visibility, we
        // should only get one node (c4) when we traverse 'heldBy'
        Iterable<Vertex> docs = invalidUserVertex
                .getVertices(Direction.IN, Ontology.DOC_HELD_BY_REPOSITORY);
        for (Vertex doc: docs) {
            System.out.println(doc.getProperty(EntityType.ID_KEY));
        }
        assertEquals(1L, Iterables.count(docs));
    }
}
