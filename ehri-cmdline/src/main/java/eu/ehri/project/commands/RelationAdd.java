package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * Add an arbitrary edge between two nodes.
 * 
 */
public class RelationAdd extends BaseCommand implements Command {

    final static String NAME = "add-rel";

    /**
     * Constructor.
     *
     */
    public RelationAdd() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("u", "unique", false,
                "Ensure the out entity only has one relationship of this type"));
        options.addOption(new Option("s", "single", false,
                "Don't create this relationship if it already exists"));
    }

    @Override
    public String getHelp() {
        return "Usage: add-rel [OPTIONS] [-Pkey=value] <source> <rel-name> <target>";
    }

    @Override
    public String getUsage() {
        return "Create a relationship between a source and a target";
    }

    /**
     * Add a relationship between two nodes.
     *
     * @throws eu.ehri.project.exceptions.ItemNotFound
     */
    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws ItemNotFound {

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        if (cmdLine.getArgList().size() < 3)
            throw new RuntimeException(getHelp());

        String src = (String)cmdLine.getArgList().get(0);
        String label = (String)cmdLine.getArgList().get(1);
        String dst = (String)cmdLine.getArgList().get(2);

        Vertex source = manager.getVertex(src);
        Vertex target = manager.getVertex(dst);

        try {
            if (cmdLine.hasOption("unique")) {
                if (!JavaHandlerUtils.addUniqueRelationship(source, target, label)) {
                    System.err.println("Relationship already exists");
                }
            } else if (cmdLine.hasOption("single")) {
                if (!JavaHandlerUtils.addSingleRelationship(source, target, label)) {
                    System.err.println("Relationship already exists");
                }
            } else {
                source.addEdge(label, target);
            }
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }

        return 0;
    }
}
