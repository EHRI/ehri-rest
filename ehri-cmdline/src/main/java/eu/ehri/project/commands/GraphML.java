package eu.ehri.project.commands;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.core.impl.GraphReindexer;

/**
 * Dump the complete graph as graphml file or import such a dump
 *
 * Example usage:
 * - stop the server
 *   $NEO4J_HOME/bin/neo4j stop
 * - save a dump
 *   ./scripts/cmd graphml -d out graph.xml 
 *   or 
 *   ./scripts/cmd graphml -d out - > graph.xml 
 * - edit it
 * - remove the graph
 *   rm -rf $NEO4J_HOME/data/graph.db
 * - load edited graph
 *   ./scripts/cmd graphml -d in graph.xml 
 * - start server
 *   $NEO4J_HOME/bin/neo4j start
 * 
 */
public class GraphML extends BaseCommand implements Command {

    final static String NAME = "graphml";

    /**
     * Constructor.
     */
    public GraphML() {
    }
    
	@Override
	public String getHelp() {
        String help = "export or import a GraphML file." + 
        		"\n" + getUsage();
        return help;
	}
	
    @Override
    public String getUsage() {
		return "Usage: graphml -d [out|in] <filename>";
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("d", true,
                "Output or input a dump"));
        
//        options.addOption(new Option("log", true,
//                "Log message for action."));
    }
    
    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

//        String logMessage = "";
//        if (cmdLine.hasOption("log")) {
//            logMessage = cmdLine.getOptionValue("log");
//            System.err.println("log: {" + logMessage + "}");
//        }
        
        if (cmdLine.getArgList().size() < 1) {
            // throw new RuntimeException(getHelp());
            throw new IllegalArgumentException();
        }
        
        String dumpMode = "out"; //defaults might also be handled by the parser?
        
        if (cmdLine.hasOption("d")) {
        	dumpMode = cmdLine.getOptionValue("d");
        }
        
        // check if option is usefull, otherwise print the help and bail out
        if (dumpMode.contentEquals("out")) {
        	saveDump(graph, cmdLine);
        } else if (dumpMode.contentEquals("in")) {
        	loadDump(graph, cmdLine);
        } else {
        	throw new IllegalArgumentException();
        }
        
        return 0;
    }

    public void saveDump(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {
        final GraphManager manager = GraphManagerFactory.getInstance(graph);
        GraphDatabaseService neo4jGraph = graph.getBaseGraph().getRawGraph();
 
//        OutputStream out = new ByteArrayOutputStream();
        GraphMLWriter writer = new GraphMLWriter(graph);
        writer.setNormalize(true);

//        writer.outputGraph(out); // Note its all in memmory now!
        
        String filepath = (String)cmdLine.getArgList().get(0);
        
        // if the file is '-' that means we do standard out
        if (filepath.contentEquals("-")) {
            // to stdout
//            System.out.println(out.toString()); 
            writer.outputGraph(System.out); 
        } else {
            // try to open or create the file for writing
//            FileWriter fileWriter = new FileWriter(filepath);
//            fileWriter.write(out.toString());   
//            fileWriter.close(); // also flushes
            OutputStream out = new FileOutputStream(filepath);
            writer.outputGraph(out); 
            out.close();
        }        
    } 
    
   public void loadDump(final FramedGraph<Neo4jGraph> graph,
           CommandLine cmdLine) throws Exception {
	   GraphMLReader reader = new GraphMLReader(graph);
	   
       String filepath = (String)cmdLine.getArgList().get(0);

	   InputStream in = new FileInputStream(filepath);
	   reader.inputGraph(in);
	   
	   GraphReindexer.reindex(graph);
   }
    
}
