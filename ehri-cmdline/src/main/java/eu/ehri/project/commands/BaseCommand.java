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

    public void printUsage() {
        // automatically generate the help statement
        System.err.println(getUsage());
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ant", options );
    }

    public final int exec(final FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception {
        setCustomOptions();
        return execWithOptions(graph, parser.parse(options, args));
    }
    public abstract int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception;

    public boolean isReadOnly() {
        return false;
    }

    protected Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
