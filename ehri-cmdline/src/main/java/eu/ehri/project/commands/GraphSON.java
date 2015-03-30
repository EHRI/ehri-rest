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
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphReindexer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Dump the complete graph as graphSON file, or import such a dump
 * <p/>
 * Example usage:
 * - stop the server
 * $NEO4J_HOME/bin/neo4j stop
 * - save a dump
 * ./scripts/cmd graphson -d out graph.json
 * or
 * ./scripts/cmd graphson -d out - > graph.json
 * - edit it
 * - remove the graph
 * rm -rf $NEO4J_HOME/data/graph.db
 * - load edited graph
 * ./scripts/cmd graphson -d in graph.json
 * - start server
 * $NEO4J_HOME/bin/neo4j start
 */
public class GraphSON extends BaseCommand implements Command {

    final static String NAME = "graphson";

    /**
     * Constructor.
     */
    public GraphSON() {
    }

    @Override
    public String getHelp() {
        return "export or import a GraphSON file.\n" + getUsage();
    }

    @Override
    public String getUsage() {
        return "Usage: graphson -d [out|in] <filename>";
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("d", true, "Output or input a dump"));
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
            CommandLine cmdLine) throws IOException {

        String filepath = (String) cmdLine.getArgList().get(0);

        // if the file is '-' that means we do standard out
        if (filepath.contentEquals("-")) {
            // to stdout
            GraphSONWriter.outputGraph(graph, System.out, GraphSONMode.EXTENDED);
        } else {
            // try to open or create the file for writing
            OutputStream out = new FileOutputStream(filepath);
            GraphSONWriter.outputGraph(graph, out, GraphSONMode.EXTENDED);
            out.close();
        }
    }

    public void loadDump(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws IOException {
        GraphSONReader reader = new GraphSONReader(graph);
        String filepath = (String) cmdLine.getArgList().get(0);

        InputStream inputStream = new FileInputStream(filepath);
        InputStream readStream = filepath.toLowerCase().endsWith(".gz")
                ? new GZIPInputStream(inputStream)
                : inputStream;
        try {
            reader.inputGraph(readStream, 1000);
            new GraphReindexer(graph).reindex();
        } finally {
            inputStream.close();
        }
    }
}
