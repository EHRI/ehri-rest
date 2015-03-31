/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphReindexer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * IMPORTANT! This command is currently not available since GraphML
 * dumps are broken with the EHRI DB since we use array properties
 * which do not save/load correctly.
 *
 * Instead the `graphson` command should be used to dump JSON.
 *
 * --------------------------------
 *
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

    public GraphML() {
    }
    
	@Override
	public String getHelp() {
        return "export or import a GraphML file." +
        		"\n" + getUsage();
	}
	
    @Override
    public String getUsage() {
		return "Usage: graphml -d [out|in] <filename>";
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("d", true,
                "Output or input a dump"));
    }
    
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
            try {
                writer.outputGraph(out);
            } finally {
                out.close();
            }
        }
    } 
    
   public void loadDump(final FramedGraph<? extends TransactionalGraph> graph,
           CommandLine cmdLine) throws Exception {
       GraphMLReader reader = new GraphMLReader(graph);
       String filepath = (String)cmdLine.getArgList().get(0);
	   InputStream inputStream = new FileInputStream(filepath);
       try {
           reader.inputGraph(inputStream);
           new GraphReindexer(graph).reindex();
       } finally {
           inputStream.close();
       }
   }
}
