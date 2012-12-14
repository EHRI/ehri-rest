package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.utils.GraphInitializer;

/**
 * Import EAD from the command line...
 * 
 */
public class Initialize extends BaseCommand implements Command {
    
    final static String NAME = "initialize";

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public Initialize() {
    }

    @Override
    protected void setCustomOptions() {
    }

    @Override
    public String getHelp() {
        return "Usage: initialize";
    }

    @Override
    public String getUsage() {
        String help = "Initialize graph DB with minimal nodes (admin account, permissions, types).";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {
        GraphInitializer initializer = new GraphInitializer(graph);
        initializer.initialize();
        return 0;
    }
}
