package eu.ehri.project.utils.fixtures.impl;

import com.google.common.io.Resources;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class YamlFixtureLoaderTest extends GraphTestBase {

    private FramedGraph<? extends TransactionalGraph> testGraph;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testGraph = getFramedGraph();
    }

    @Test
    public void testWithInitializationError() throws Exception {
        try {
            // NB: An initialized Neo4j 1.9 graph has a root node
            assertEquals(1, getNodeCount(testGraph));
            new GraphInitializer(testGraph).initialize();
            FixtureLoader loader = new YamlFixtureLoader(testGraph);
            loader.loadTestData();
            fail("Loading fixtures on an initialized graph should have" +
                    " thrown an integrity error");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Integrity error"));
        }
    }

    @Test
    public void testWithInitialization() throws Exception {
        // NB: An initialized Neo4j 1.9 graph has a root node
        assertEquals(1, getNodeCount(testGraph));
        new GraphInitializer(testGraph).initialize();
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.setInitializing(false);
        loader.loadTestData();
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataFile() throws Exception {
        assertEquals(1, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData(getFixtureFilePath("testdata.yaml"));
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataResource() throws Exception {
        assertEquals(1, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData("testdata.yaml");
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataStream() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("testdata.yaml");
        assertNotNull(inputStream);

        assertEquals(1, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData(inputStream);
        assertTrue(getNodeCount(testGraph) > 1);
    }

    private static String getFixtureFilePath(final String resourceName) throws Exception {
        URL resource = Resources.getResource(resourceName);
        return new File(resource.toURI()).getAbsolutePath();
    }
}
