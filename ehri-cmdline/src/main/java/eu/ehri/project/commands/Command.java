package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.CommandLine;

public interface Command {
    /**
     * Get information about the functionality of the command.
     *
     * @return a help text
     */
    public String getHelp();

    /**
     * Get information about the usage of the command, including
     * required and/or optional parameters.
     * @return a usage text
     */
    public String getUsage();

    /**
     * Execute this command with the given command line options.
     * @param graph
     * @param cmdLine
     * @return a status code (0 = success)
     * @throws Exception
     */
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph, CommandLine cmdLine) throws Exception;
    public int exec(FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception;
}
