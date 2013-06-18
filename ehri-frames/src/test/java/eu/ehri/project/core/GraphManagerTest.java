package eu.ehri.project.core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.TransactionalGraph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;

/**
 * 
 * @author paulboon
 * 
 */
public class GraphManagerTest {
    private static final String NON_EXISTING_ID = "non-existing-id-12345678";
    private static final String TEST_ID1 = "12345678";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private static final EntityClass TEST_TYPE = EntityClass.USER_PROFILE;


    protected FramedGraph<Neo4jGraph> graph;
    protected GraphManager manager;

    /**
     * Note that there is only one implementation that I can test:
     * SingleIndexGraphManager but the factory handles that.
     */
    @Before
    public void setUp() {
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                        .newGraphDatabase()));
        manager = GraphManagerFactory.getInstance(graph);
    }

    @After
    public void tearDown() {
        graph.shutdown();
    }

    @Test
    public void testNonExistingVertex() {
        boolean exists = manager.exists(NON_EXISTING_ID);
        assertFalse(exists);
    }

    @Test(expected = ItemNotFound.class)
    public void testDeleteNonExistingVertex() throws ItemNotFound {
        manager.deleteVertex(NON_EXISTING_ID);
    }

    @Test(expected = ItemNotFound.class)
    public void testGetNonExistingVertex() throws ItemNotFound {
        manager.getVertex(NON_EXISTING_ID);
    }

    @Test
    public void testCreateVertex() throws Exception {
        @SuppressWarnings("serial")
        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put(TEST_KEY, TEST_VALUE);
            }
        };

        Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);

        assertEquals(TEST_ID1, manager.getId(vertex));
        assertEquals(TEST_TYPE, manager.getEntityClass(vertex));
        assertEquals(TEST_VALUE, vertex.getProperty(TEST_KEY));

        assertTrue(manager.exists(TEST_ID1));
        // now get it and test again
        vertex = manager.getVertex(TEST_ID1);
        assertEquals(TEST_ID1, manager.getId(vertex));
        assertEquals(TEST_TYPE, manager.getEntityClass(vertex));
        assertEquals(TEST_VALUE, vertex.getProperty(TEST_KEY));
    }

    @Test
    public void testDeleteVertex() throws IntegrityError, ItemNotFound {
        @SuppressWarnings("serial")
        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put(TEST_KEY, TEST_VALUE);
            }
        };

        manager.createVertex(TEST_ID1, TEST_TYPE, data);
        manager.deleteVertex(TEST_ID1); // don't want the exeption here

        assertFalse(manager.exists(TEST_ID1));

        try {
            manager.getVertex(TEST_ID1);
            fail(String
                    .format("Fetching vertex with id '%s' should have failed since it was deleted.",
                            TEST_ID1));
        } catch (ItemNotFound e) {
            // ignore, this is OK
        }
    }

    @Test
    public void testUpdateVertex() throws Exception {
        @SuppressWarnings("serial")
        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put(TEST_KEY, TEST_VALUE);
            }
        };

        Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);

        final String NEW_TEST_KEY = "newTestKey";
        final String NEW_TEST_VALUE = "newTestValue";

        // change a value of existing key
        data.put(TEST_KEY, NEW_TEST_VALUE);
        manager.updateVertex(TEST_ID1, TEST_TYPE, data);
        vertex = manager.getVertex(TEST_ID1);
        assertEquals(NEW_TEST_VALUE, vertex.getProperty(TEST_KEY));

        // add a new key, value pair
        data.put(NEW_TEST_KEY, NEW_TEST_VALUE);
        manager.updateVertex(TEST_ID1, TEST_TYPE, data);
        vertex = manager.getVertex(TEST_ID1);
        assertEquals(NEW_TEST_VALUE, vertex.getProperty(TEST_KEY));
        assertEquals(NEW_TEST_VALUE, vertex.getProperty(NEW_TEST_KEY));

        // remove a key, value pair
        data.remove(TEST_KEY);
        manager.updateVertex(TEST_ID1, TEST_TYPE, data);
        vertex = manager.getVertex(TEST_ID1);
        assertEquals(NEW_TEST_VALUE, vertex.getProperty(NEW_TEST_KEY));
        assertEquals(null, vertex.getProperty(TEST_KEY));
    }

    // TODO copy and change the other tests

    @SuppressWarnings("serial")
    @Test
    public void testSelectiveIndexing() throws IndexNotFoundException,
            IntegrityError {
        // We need to create one first, sorry
        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put("name", "joe");
                put("age", 32);
                put("height", "5.11");
            }
        };
        List<String> keys = new LinkedList<String>() {
            {
                add("name");
                add("age");
            }
        };

        Vertex joe = manager.createVertex(TEST_ID1, TEST_TYPE, data, keys);
        graph.getBaseGraph().commit();

        // try and find joe via name and age...
        CloseableIterable<Vertex> query1 = manager.getVertices("name",
                data.get("name"), TEST_TYPE);
        assertTrue(query1.iterator().hasNext());
        Vertex joe1 = query1.iterator().next();
        assertEquals(joe, joe1);

        CloseableIterable<Vertex> query2 = manager.getVertices("age",
                data.get("age"), TEST_TYPE);
        assertTrue(query2.iterator().hasNext());
        Vertex joe2 = query2.iterator().next();
        assertEquals(joe, joe2);

        // Query by height should fail...
        CloseableIterable<Vertex> query3 = manager.getVertices("height",
                data.get("height"), TEST_TYPE);
        assertFalse(query3.iterator().hasNext());
    }
}
