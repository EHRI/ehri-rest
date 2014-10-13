package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphReindexer;
import org.apache.commons.cli.CommandLine;

/**
 * Import EAD from the command line...
 * 
 */
public class Reindex extends BaseCommand implements Command {

    final static String NAME = "reindex";


    public Reindex() {
    }

    @Override
    protected void setCustomOptions() {
    }

    @Override
    public String getHelp() {
        return "Usage: reindex";
    }

    @Override
    public String getUsage() {
        return "Drop and rebuild the (internal) graph index.";
    }


    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph, CommandLine cmdLine) throws Exception {
        GraphReindexer reIndexer = new GraphReindexer(graph);
        reIndexer.reindex();
        return 0;
    }
}
