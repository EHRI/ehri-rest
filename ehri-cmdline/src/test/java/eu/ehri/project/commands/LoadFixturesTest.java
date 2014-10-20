package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.GraphInitializer;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LoadFixturesTest extends GraphTestBase {

    private FramedGraph<? extends TransactionalGraph> testGraph;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testGraph = getFramedGraph();
        new GraphInitializer(testGraph).initialize();
    }

    @Test
    public void testWithResourceFilePath() throws Exception {
        List<VertexProxy> before = getGraphState(testGraph);
        String path = getFixtureFilePath("testdata.yaml");
        String[] args = new String[]{path};
        LoadFixtures load = new LoadFixtures();
        CommandLine cmdLine = load.getCmdLine(args);
        assertEquals(0, load.execWithOptions(testGraph, cmdLine));

        // Check some nodes have been added...
        List<VertexProxy> after = getGraphState(testGraph);
        GraphDiff graphDiff = diffGraph(before, after);
        assertTrue(graphDiff.added.size() > 0);
        assertFalse(graphDiff.removed.size() > 0);

    }
}
