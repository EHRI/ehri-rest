package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.io.File;
import java.io.FileInputStream;

/**
 * Import EAD from the command line...
 * 
 */
public class LoadFixtures extends BaseCommand implements Command {

    final static String NAME = "load-fixtures";

    /**
     * Constructor.
     * 
     */
    public LoadFixtures() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("init",
                "Initialize graph before loading fixtures"));
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
     * @throws Exception
     */
    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {
        boolean initialize = cmdLine.hasOption("init");
        FixtureLoader loader = FixtureLoaderFactory.getInstance(graph, initialize);
        if (cmdLine.getArgList().size() == 1) {
            String path = (String)cmdLine.getArgs()[0];
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                throw new RuntimeException(String.format(
                        "Fixture file: '%s does not exist or is not a file", path));
            }
            System.err.println("Loading fixture file: " + path);
            loader.loadTestData(new FileInputStream(file));
        } else {
            // Load default fixtures...
            loader.loadTestData();
        }

        return 0;
    }
}
