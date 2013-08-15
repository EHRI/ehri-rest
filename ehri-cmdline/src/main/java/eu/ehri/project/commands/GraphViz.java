package eu.ehri.project.commands;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Dump a particular subgraph as a DOT file.
 *
 */
public class GraphViz extends BaseCommand implements Command {

    final static String NAME = "graphviz";

    /**
     * Constructor.
     */
    public GraphViz() {
    }

    @Override
    public String getHelp() {
        return "Usage: graphviz [OPTIONS] <type> <identifier>";
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("r", "relationship", true,
                "A relationship to include in the graph"));
    }

    @Override
    public String getUsage() {
        String help = "Dump a dot file.";
        return help;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        final GraphManager manager = GraphManagerFactory.getInstance(graph);
        GraphDatabaseService neo4jGraph = ((Neo4jGraph)graph.getBaseGraph()).getRawGraph();

        // Cmdline arguments should be a node and a list of relationship types
        // to traverse.
        if (cmdLine.getArgList().size() < 2)
            throw new RuntimeException(getHelp());

        List<Node> nodes = Lists.newArrayList();
        for (int i = 0; i < cmdLine.getArgs().length; i++) {
            nodes.add(neo4jGraph.getNodeById(
                    (Long)manager.getVertex(cmdLine.getArgs()[i]).getId()));
        }

        DynamicRelationshipType[] rels = new DynamicRelationshipType[
                cmdLine.getOptionValues("relationship").length];
        if (cmdLine.hasOption("relationship")) {
            int i = 0;
            for (String rel : cmdLine.getOptionValues("relationship")) {
                rels[i++] = DynamicRelationshipType.withName(rel);
            }
        }

        OutputStream out = new ByteArrayOutputStream();
        GraphvizWriter writer = new GraphvizWriter();

        System.out.println("Nodes: " + nodes);
        System.out.println("Rels:  " + Lists.newArrayList(rels));

        writer.emit(out, Walker.crosscut(nodes, rels));
        System.out.println(out.toString());
        return 0;
    }
}
