package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class GraphSONTest extends GraphTestBase {
    protected FixtureLoader helper;

    @Test
    public void testSaveDumpAndRead() throws Exception {

        // 1. Load the (YAML) fixtures into a graph.
        // 2. Dump it as JSON.
        // 3. Load the JSON into another graph.
        // 4. Compare the before and after graphs and check
        //     nothing's changed.

        // Setup will create the database but not load the fixtures...
        FramedGraph<? extends TransactionalGraph> graph1 = getTestGraph();
        helper = FixtureLoaderFactory.getInstance(graph1);
        helper.loadTestData();

        List<VertexProxy> graphState1 = getGraphState(graph1);

        File temp = File.createTempFile("temp-file-name", ".tmp");
        temp.deleteOnExit();
        assertEquals(0L, temp.length());

        String filePath = temp.getAbsolutePath();
        String[] outArgs = new String[]{"-d", "out", filePath};

        GraphSON graphSON = new GraphSON();
        CommandLine outCmdLine = graphSON.getCmdLine(outArgs);

        assertEquals(0, graphSON.execWithOptions(graph1, outCmdLine));
        graph1.shutdown();

        assertTrue(temp.exists());
        assertTrue(temp.length() > 0L);

        FramedGraph<? extends TransactionalGraph> graph2 = getTestGraph();

        String[] inArgs = new String[]{"-d", "in", filePath};
        CommandLine inCmdLine = graphSON.getCmdLine(inArgs);
        assertEquals(0, graphSON.execWithOptions(graph2, inCmdLine));

        List<VertexProxy> graphState2 = getGraphState(graph2);

        GraphDiff graphDiff = diffGraph(graphState1, graphState2);
        assertEquals(0, graphDiff.added.size());
        assertEquals(0, graphDiff.removed.size());
    }

    private FramedGraph<? extends TransactionalGraph> getTestGraph() {
        FramedGraph<? extends TransactionalGraph> graph1 = getFramedGraph();
        // This is a bit gnarly, but Neo4j 1.9 graphs have a 0 node which
        // we don't need - when loading and dumping into a new graph it
        // will throw off the counts, so delete it...
        graph1.getBaseGraph().removeVertex(graph1.getVertex(0));
        return graph1;
    }
}
