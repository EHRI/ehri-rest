package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.core.impl.GraphReindexer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

    private static final String INDEX_NAME = "entities"; // FIXME!!

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
    }
    
    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1) {
            throw new MissingArgumentException("Graph file path missing");
        }
        
        String dumpMode = "out"; //defaults might also be handled by the parser?
        
        if (cmdLine.hasOption("d")) {
        	dumpMode = cmdLine.getOptionValue("d");
        }
        
        // check if option is useful, otherwise print the help and bail out
        if (dumpMode.contentEquals("out")) {
        	saveDump(graph, cmdLine);
        } else if (dumpMode.contentEquals("in")) {
        	loadDump(graph, cmdLine);
        } else {
            throw new UnrecognizedOptionException("Unrecognised dump mode: '" + dumpMode + "'");
        }
        
        return 0;
    }

    public void saveDump(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {
        final GraphManager manager = GraphManagerFactory.getInstance(graph);

        GraphMLWriter writer = new GraphMLWriter(graph);
        writer.setNormalize(true);

        String filepath = (String)cmdLine.getArgList().get(0);
        
        // if the file is '-' that means we do standard out
        if (filepath.contentEquals("-")) {
            // to stdout
            writer.outputGraph(System.out);
        } else {
            // try to open or create the file for writing
            OutputStream out = new FileOutputStream(filepath);
            writer.outputGraph(out); 
            out.close();
        }        
    } 
    
   public void loadDump(final FramedGraph<? extends TransactionalGraph> graph,
           CommandLine cmdLine) throws Exception {
       GraphMLReader reader = new GraphMLReader(graph);
       String filepath = (String)cmdLine.getArgList().get(0);
	   InputStream in = new FileInputStream(filepath);
	   reader.inputGraph(in);
	   new GraphReindexer(graph).reindex(INDEX_NAME);
   }
}
