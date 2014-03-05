package eu.ehri.project.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import eu.ehri.project.core.impl.BasicGraphManager;
import eu.ehri.project.core.impl.SingleIndexGraphManager;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Graph Manager tests. These do a bit of funky
 * reflectiveness to create a re-useable suite that
 * can be run for each implementation.
 *
 * @author paulboon
 * @author http://github.com/mikesname
 */
public class GraphManagerTest {
    private static final String NON_EXISTING_ID = "non-existing-id-12345678";
    private static final String TEST_ID1 = "12345678";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private static final EntityClass TEST_TYPE = EntityClass.USER_PROFILE;

    @Test
    public void testBasicGraphManager() throws Throwable {
        new Suite(BasicGraphManager.class).run();
    }

    @Test
    public void testSingleIndexGraphManager() throws Throwable {
        new Suite(SingleIndexGraphManager.class).run();
    }

    /**
     * Test suite implementation.
     */
    private static class Suite {
        private GraphManager manager;
        private FramedGraph<? extends TransactionalGraph> graph;
        private final Class<? extends GraphManager> cls;

        public Suite(Class<? extends GraphManager> cls) {
            this.cls = cls;
        }

        public void run() throws Throwable {
            for (Method method : this.getClass().getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                if (annotation != null) {
                    Class<? extends Throwable> expected = annotation.expected();
                    System.err.println(" --- " + cls.getSimpleName() + " invoking test: " + method.getName());
                    try {
                        setUp();
                        method.invoke(this);
                        tearDown();
                    } catch (InvocationTargetException e) {
                        if (expected == null
                                || !e.getTargetException().getClass().equals(expected)) {
                            throw e.getCause();
                        }
                    }
                }
            }
        }

        @Before
        public void setUp() throws Exception {
            // NB: Not loading modules to allow use of frames methods, like GremlinGroovy
            graph = new FramedGraphFactory().create(new Neo4jGraph(
                    new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                            .newGraphDatabase()));
            manager = cls.getConstructor(FramedGraph.class).newInstance(graph);
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
            shouldHaveThrown(ItemNotFound.class);
        }

        @Test(expected = ItemNotFound.class)
        public void testGetNonExistingVertex() throws ItemNotFound {
            manager.getVertex(NON_EXISTING_ID);
            shouldHaveThrown(ItemNotFound.class);
        }

        private void shouldHaveThrown(Class<?> cls) {
            fail("Should have thrown: " + cls.getCanonicalName());
        }

        @Test
        public void testCreateVertex() throws Exception {
            Map<String, String> data = ImmutableMap.of(TEST_KEY, TEST_VALUE);
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);

            assertEquals(TEST_ID1, manager.getId(vertex));
            assertEquals(TEST_TYPE, manager.getEntityClass(vertex));
            assertEquals(TEST_VALUE, vertex.getProperty(TEST_KEY));

            assertTrue(manager.exists(TEST_ID1));

            assertEquals(TEST_ID1, manager.getId(vertex));
            assertEquals(TEST_TYPE, manager.getEntityClass(vertex));
            assertEquals(TEST_VALUE, vertex.getProperty(TEST_KEY));
        }

        @Test
        public void testDeleteVertex() throws IntegrityError, ItemNotFound {
            Map<String, String> data = ImmutableMap.of(TEST_KEY, TEST_VALUE);
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

        @Test(expected = ItemNotFound.class)
        public void testUpdateNonExistentVertex() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            manager.updateVertex("i-dont-exist", TEST_TYPE, data);
        }

        @Test
        public void testUpdateVertex() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);

            final String NEW_TEST_KEY = "newTestKey";
            final String NEW_TEST_VALUE = "newTestValue";

            // change a value of existing key
            data.put(TEST_KEY, NEW_TEST_VALUE);
            manager.updateVertex(TEST_ID1, TEST_TYPE, data);

            assertEquals(NEW_TEST_VALUE, vertex.getProperty(TEST_KEY));

            // add a new key, value pair
            data.put(NEW_TEST_KEY, NEW_TEST_VALUE);
            manager.updateVertex(TEST_ID1, TEST_TYPE, data);

            assertEquals(NEW_TEST_VALUE, vertex.getProperty(TEST_KEY));
            assertEquals(NEW_TEST_VALUE, vertex.getProperty(NEW_TEST_KEY));

            // remove a key, value pair
            data.remove(TEST_KEY);
            manager.updateVertex(TEST_ID1, TEST_TYPE, data);

            assertEquals(NEW_TEST_VALUE, vertex.getProperty(NEW_TEST_KEY));
            assertEquals(null, vertex.getProperty(TEST_KEY));
        }

        @Test
        public void testUpdateVertexWithMetadata() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);

            String testMetaKey = "_metakey";
            String testMetaValue = "test-value";
            vertex.setProperty(testMetaKey, testMetaValue);

            String NEW_TEST_VALUE = "newTestValue";

            // change a value of existing key
            data.put(TEST_KEY, NEW_TEST_VALUE);
            manager.updateVertex(TEST_ID1, TEST_TYPE, data);
            vertex = manager.getVertex(TEST_ID1);
            assertEquals(NEW_TEST_VALUE, vertex.getProperty(TEST_KEY));

            // Check the metadata remains unharmed
            assertEquals(testMetaValue, vertex.getProperty(testMetaKey));
        }

        @Test
        public void testSelectiveIndexing() throws IndexNotFoundException,
                IntegrityError {
            // We need to create one first, sorry
            Map<String, ?> data = ImmutableMap.of(
                    "name", "joe",
                    "age", 32,
                    "height", "5.11");
            List<String> keys = Lists.newArrayList("name", "age");

            Vertex joe = manager.createVertex(TEST_ID1, TEST_TYPE, data, keys);

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
}
