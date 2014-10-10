package eu.ehri.project.commands;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.*;

public abstract class BaseCommand {
    
    Options options = new Options();
    CommandLineParser parser = new PosixParser();
        
    protected void setCustomOptions() {}

    public abstract String getHelp();
    public abstract String getUsage();

    public final int exec(final FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception {
        setCustomOptions();
        return execWithOptions(graph, parser.parse(options, args));
    }
    public abstract int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception;

    /**
     * Utility to get a parsed command line from some args. This is
     * mainly helpful for testing.
     * @param args A list of arg strings
     * @return The parsed command line
     */
    public CommandLine getCmdLine(String[] args) throws ParseException {
        setCustomOptions();
        return parser.parse(options, args);
    }

    protected Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
