package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

public abstract class BaseCommand {
    
    Options options = new Options();
    CommandLineParser parser = new PosixParser();
        
    protected void setCustomOptions() {};    
    public abstract String getHelp();
    public abstract String getUsage();
    public final int exec(final FramedGraph<Neo4jGraph> graph, String[] args) throws Exception {
        setCustomOptions();
        return execWithOptions(graph, parser.parse(options, args));
    }
    public abstract int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception;
}
