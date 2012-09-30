package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.utils.FixtureLoader;

/**
 * Import EAD from the command line...
 * 
 */
public class LoadFixtures extends BaseCommand implements Command {

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public LoadFixtures() {
    }

    @Override
    protected void setCustomOptions() {
    }

    @Override
    public String getHelp() {
        return "Usage: load-fixtures";
    }

    @Override
    public String getUsage() {
        String help = "Load the fixtures into the database.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     * 
     * @param args
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {
        FixtureLoader loader = new FixtureLoader(graph);
        loader.loadTestData();

        return 0;
    }
}
