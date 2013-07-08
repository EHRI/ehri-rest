package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.CommandLine;

public interface Command {
    public String getHelp();
    public String getUsage();
    public boolean isReadOnly();
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph, CommandLine cmdLine) throws Exception;
    public int exec(FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception;
}
