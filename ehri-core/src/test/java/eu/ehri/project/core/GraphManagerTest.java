/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import eu.ehri.project.core.impl.BlueprintsGraphManager;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

/**
 * Graph Manager tests. These do a bit of funky
 * reflectiveness to create a re-useable suite that
 * can be run for each implementation.
 */
public class GraphManagerTest {
    private static final String NON_EXISTING_ID = "non-existing-id-12345678";
    private static final String TEST_ID1 = "12345678";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private static final EntityClass TEST_TYPE = EntityClass.USER_PROFILE;

    @Test
    public void testBasicGraphManager() throws Throwable {
        new Suite(BlueprintsGraphManager.class).run();
    }

    @Test
    public void testSingleIndexGraphManager() throws Throwable {
        new Suite(Neo4jGraphManager.class).run();
    }

    /**
     * Test suite implementation.
     */
    private static class Suite {
        private GraphManager manager;
        private FramedGraph<? extends TransactionalGraph> graph;
        private final Class<? extends GraphManager> cls;
        Path tempDir;

        public Suite(Class<? extends GraphManager> cls) {
            this.cls = cls;
        }

        public void run() throws Throwable {
            for (Method method : this.getClass().getDeclaredMethods()) {
                Test annotation = method.getAnnotation(Test.class);
                Ignore ignore = method.getAnnotation(Ignore.class);
                if (annotation != null && ignore == null) {
                    Class<? extends Throwable> expected = annotation.expected();
                    System.err.println(" --- " + cls.getSimpleName() + " invoking test: " + method.getName());
                    setUp();
                    try {
                        method.invoke(this);
                    } catch (InvocationTargetException e) {
                        if (!e.getTargetException().getClass().equals(expected)) {
                            throw e.getCause();
                        }
                    } finally {
                        tearDown();
                    }
                }
            }
        }

        @Before
        public void setUp() throws Exception {
            // NB: Not loading modules to allow use of frames methods, like GremlinGroovy
            tempDir = Files.createTempDirectory("neo4j-tmp");
            graph = new FramedGraphFactory().create(new Neo4j2Graph(tempDir.toString()));
            manager = cls.getConstructor(FramedGraph.class).newInstance(graph);
        }

        @After
        public void tearDown() {
            graph.shutdown();
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            assertNull(vertex.getProperty(TEST_KEY));
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
        public void testSetProperty() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);
            manager.setProperty(vertex, TEST_KEY, "foo");
            assertEquals("foo", vertex.getProperty(TEST_KEY));
        }

        @Test
        public void testGetProperties() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);
            manager.setProperty(vertex, TEST_KEY, "foo");
            Map<String, Object> properties = manager.getProperties(vertex);
            assertEquals("foo", properties.get(TEST_KEY));
        }

        @Test(expected = IllegalArgumentException.class)
        public void testSetPropertyWithBlankPropArg() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);
            manager.setProperty(vertex, "", "foo");
        }

        @Test(expected = IllegalArgumentException.class)
        public void testSetPropertyWithBadPropArg() throws Exception {
            Map<String, String> data = Maps
                    .newHashMap(ImmutableMap.of(TEST_KEY, TEST_VALUE));
            Vertex vertex = manager.createVertex(TEST_ID1, TEST_TYPE, data);
            manager.setProperty(vertex, EntityType.ID_KEY, "foo");
        }
    }
}
