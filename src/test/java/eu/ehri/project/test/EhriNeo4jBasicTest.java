package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.IndexNotFoundException;

public class EhriNeo4jBasicTest {
    protected GraphDatabaseService graphDb;
    protected GraphHelpers helpers;

    private static final String TEST_VERTEX_INDEX_NAME = "testVertexIndex";
    private static final String TEST_EDGE_INDEX_NAME = "testEdgeIndex";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";

    /**
     * Create temporary database for each unit test.
     */
    @Before
    public void prepareTestDatabase() {
        graphDb = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder().newGraphDatabase();
        helpers = new GraphHelpers(graphDb);
        helpers.createIndex(TEST_VERTEX_INDEX_NAME, Vertex.class);
        helpers.createIndex(TEST_EDGE_INDEX_NAME, Edge.class);
    }

    /**
     * Shutdown the database.
     */
    @After
    public void destroyTestDatabase() {
        graphDb.shutdown();
    }

    @Test
    public void testCreateIndexedVertex() throws IndexNotFoundException {
        // TODO test if it handles null or empty strings etc.

        Vertex indexedVertex = createIndexedVertexWithProperty(TEST_KEY,
                TEST_VALUE);
        assertEquals((String) indexedVertex.getProperty(TEST_KEY), TEST_VALUE);

        // do we have a node with that id and property in the neo4j database?
        Node foundNode = graphDb.getNodeById((Long) indexedVertex.getId());
        assertEquals(foundNode.getId(), indexedVertex.getId());
        assertEquals((String) foundNode.getProperty(TEST_KEY), TEST_VALUE);
    }

    @Test(expected = org.neo4j.graphdb.NotFoundException.class)
    public void testDeleteIndexedVertex() throws IndexNotFoundException {
        // We need to create one first, sorry
        Vertex indexedVertex = createIndexedVertexWithProperty(TEST_KEY,
                TEST_VALUE);
        Long vertexId = (Long) indexedVertex.getId();

        helpers.deleteVertex((Long) indexedVertex.getId());

        // we should not find the node in the neo4j database anymore
        graphDb.getNodeById(vertexId);
    }

    // TODO test deleting non-existing vertex, and other bad input

    @Test
    public void testUpdateIndexedVertex() throws IndexNotFoundException {
        // We need to create one first, sorry
        Vertex indexedVertex = createIndexedVertexWithProperty(TEST_KEY,
                TEST_VALUE);
        Long vertexId = (Long) indexedVertex.getId();

        final String NEW_TEST_KEY = "newTestKey";
        final String NEW_TEST_VALUE = "newTestKey";

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(TEST_KEY, NEW_TEST_VALUE); // change existing property
        data.put(NEW_TEST_KEY, NEW_TEST_VALUE); // add a new property

        Vertex updatedIndexedVertex = helpers.updateIndexedVertex(vertexId,
                data, TEST_VERTEX_INDEX_NAME);
        String changedValue = (String) updatedIndexedVertex
                .getProperty(TEST_KEY);

        assertEquals(changedValue, NEW_TEST_VALUE);
        String newValue = (String) updatedIndexedVertex
                .getProperty(NEW_TEST_KEY);
        assertEquals(newValue, NEW_TEST_VALUE);
    }

    @Test
    public void testCreateIndexedEdge() throws IndexNotFoundException {
        // TODO test if it handles null or empty strings etc.

        // create two Vertices first
        Vertex outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        Vertex inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        final String TEST_TYPE = "testType";

        // check that vertices have no relations before creation
        assertFalse(hasRelationship(outV));
        assertFalse(hasRelationship(inV));

        Edge relationship = helpers.createIndexedEdge(outV.getId(),
                inV.getId(), TEST_TYPE, createTestData(TEST_KEY, TEST_VALUE),
                TEST_EDGE_INDEX_NAME);
        Long id = (Long) relationship.getId();

        // check that vertices have relations after creation
        assertTrue(hasRelationship(outV));
        assertTrue(hasRelationship(inV));

        // do we have a relationship with that id and property etc. in the neo4j
        // database?
        Relationship foundRelationship = graphDb.getRelationshipById(id);
        assertEquals((Long) foundRelationship.getId(), id);
        assertEquals(foundRelationship.getStartNode().getId(), outV.getId());
        assertEquals(foundRelationship.getEndNode().getId(), inV.getId());
        assertEquals(foundRelationship.getType().toString(), TEST_TYPE); // ?
        assertEquals((String) foundRelationship.getProperty(TEST_KEY),
                TEST_VALUE);
    }

    @Test(expected = org.neo4j.graphdb.NotFoundException.class)
    public void testDeleteIndexedEdge() throws IndexNotFoundException {
        // TODO test if it handles null or empty strings etc.

        // create two Vertices first
        Vertex outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        Vertex inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        final String TEST_TYPE = "testType";

        // create the Edge
        Edge relationship = helpers.createIndexedEdge(outV.getId(),
                inV.getId(), TEST_TYPE, createTestData(TEST_KEY, TEST_VALUE),
                TEST_EDGE_INDEX_NAME);
        Long id = (Long) relationship.getId();

        // check that vertices have relations before delete
        assertTrue(hasRelationship(outV));
        assertTrue(hasRelationship(inV));

        // delete it
        helpers.deleteEdge(id);

        // check that vertices have no relations after delete
        assertFalse(hasRelationship(outV));
        assertFalse(hasRelationship(inV));

        // we should not have a relationship with that id and property etc. in
        // the neo4j database
        graphDb.getRelationshipById(id);
    }

    @Test
    public void testUpdateIndexedEdge() throws IndexNotFoundException {
        // TODO test if it handles null or empty strings etc.

        final String NEW_TEST_KEY = "newTestKey";
        final String NEW_TEST_VALUE = "newTestKey";

        // create two Vertices first
        Vertex outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        Vertex inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
        final String TEST_TYPE = "testType";

        // create the Edge
        Edge relationship = helpers.createIndexedEdge(outV.getId(),
                inV.getId(), TEST_TYPE, createTestData(TEST_KEY, TEST_VALUE),
                TEST_EDGE_INDEX_NAME);
        Long id = (Long) relationship.getId();

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(TEST_KEY, NEW_TEST_VALUE); // change existing property
        data.put(NEW_TEST_KEY, NEW_TEST_VALUE); // add a new property

        Edge updatedIndexedEdge = helpers.updateIndexedEdge(id, data,
                TEST_EDGE_INDEX_NAME);
        String changedValue = (String) updatedIndexedEdge.getProperty(TEST_KEY);

        assertEquals(changedValue, NEW_TEST_VALUE);
        String newValue = (String) updatedIndexedEdge.getProperty(NEW_TEST_KEY);
        assertEquals(newValue, NEW_TEST_VALUE);
    }

    /***
     * index
     * 
     * @throws IndexNotFoundException
     ***/

    @Test
    public void testGetOrCreateVertexIndex() {
        final String NEW_INDEX_NAME = "newTestIndex";

        // make sure we don't have it
        Index<Vertex> index = null;
        try {
            index = helpers.getIndex(NEW_INDEX_NAME, Vertex.class);
        } catch (IndexNotFoundException e) {
        }

        // create it
        index = helpers.getOrCreateIndex(NEW_INDEX_NAME, Vertex.class);
        assertFalse(index == null);

        // and check if we can find it via get
        try {
            index = helpers.getIndex(NEW_INDEX_NAME, Vertex.class);
        } catch (IndexNotFoundException e) {
            fail("Created index does not exist.");
        }
    }

    @Test
    public void testGetOrCreateEdgeIndex() {
        final String NEW_INDEX_NAME = "newTestIndex";

        // make sure we don't have it
        Index<Edge> index;
        try {
            index = helpers.getIndex(NEW_INDEX_NAME, Edge.class);
        } catch (IndexNotFoundException e) {
        }

        // create it
        index = helpers.getOrCreateIndex(NEW_INDEX_NAME, Edge.class);
        assertFalse(index == null);

        // and check if we can find it via get
        try {
            index = helpers.getIndex(NEW_INDEX_NAME, Edge.class);
        } catch (IndexNotFoundException e) {
            fail("Created index does not exist.");
        }
    }

    /***
     * helpers
     * 
     * @throws IndexNotFoundException
     ***/

    private Vertex createIndexedVertexWithProperty(String key, String value)
            throws IndexNotFoundException {
        return helpers.createIndexedVertex(createTestData(key, value),
                TEST_VERTEX_INDEX_NAME);
    }

    private Map<String, Object> createTestData(String key, String value) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);

        return data;
    }

    private Boolean hasRelationship(Vertex vertex) {
        return vertex.getEdges(Direction.BOTH).iterator().hasNext();
    }
}
