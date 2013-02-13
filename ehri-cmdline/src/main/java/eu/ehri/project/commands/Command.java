package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

public interface Command {
    public String getHelp();
    public String getUsage();
    public boolean isReadOnly();
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception;
    public int exec(FramedGraph<Neo4jGraph> graph, String[] args) throws Exception;
}
