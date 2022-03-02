/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

/**
 * Dump the complete graph as graphSON file, or import such a dump
 * <p>
 * Example usage:
 * <pre>
 *     <code>
 * # stop the server
 * $NEO4J_HOME/bin/neo4j stop
 * # save a dump
 * ./scripts/cmd graphson -d out graph.json
 * # or
 * ./scripts/cmd graphson -d out - &gt; graph.json
 * # edit it
 * # remove the graph
 * rm -rf $NEO4J_HOME/data/graph.db
 * # load edited graph
 * ./scripts/cmd graphson -d in graph.json
 * # start server
 * $NEO4J_HOME/bin/neo4j start
 *     </code>
 * </pre>
 */
public class GraphSON extends BaseCommand {

    final static String NAME = "graphson";

    @Override
    public String getHelp() {
        return "Load or dump GraphSON data.";
    }

    @Override
    public String getHelpFooter() {
        return "Default is to dump to stdout";
    }

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] [--load <filename>|--dump <filename>]", NAME);
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder("l")
                .hasArg().type(String.class)
                .longOpt("load")
                .desc("Load a dump file").build());
        options.addOption(Option.builder("d")
                .hasArg().type(String.class)
                .longOpt("dump")
                .desc("Save a dump file").build());
        options.addOption(Option.builder("b")
                .hasArg().type(Integer.class)
                .longOpt("buffer-size")
                .desc("Transaction buffer size").build());
        options.addOption(Option.builder()
                .longOpt("skip-setting-labels")
                .desc("Initialize indices after load").build());
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        // check if option is useful, otherwise print the help and bail out
        if (cmdLine.hasOption("dump")) {
            saveDump(graph, cmdLine.getOptionValue("dump"), cmdLine);
        } else if (cmdLine.hasOption("load")) {
            loadDump(graph, cmdLine.getOptionValue("load"), cmdLine);
        } else {
            saveDump(graph, "-", cmdLine);
        }

        return 0;
    }

    private void saveDump(FramedGraph<?> graph,
            String filePath, CommandLine cmdLine) throws IOException {

        // if the file is '-' that means we do standard out
        if (filePath.contentEquals("-")) {
            // to stdout
            GraphSONWriter.outputGraph(graph, System.out, GraphSONMode.EXTENDED);
        } else {
            // try to open or create the file for writing
            OutputStream out = Files.newOutputStream(Paths.get(filePath));
            GraphSONWriter.outputGraph(graph, out, GraphSONMode.EXTENDED);
            out.close();
        }
    }

    private void loadDump(FramedGraph<?> graph,
            String filePath, CommandLine cmdLine) throws Exception {
        GraphSONReader reader = new GraphSONReader(graph);

        InputStream readStream = System.in;
        if (!filePath.equals("-")) {
            InputStream inputStream = Files.newInputStream(Paths.get(filePath));
            readStream = filePath.toLowerCase().endsWith(".gz")
                    ? new GZIPInputStream(inputStream)
                    : inputStream;
        }

        int bufferSize = cmdLine.hasOption("buffer-size")
                ? Integer.parseInt(cmdLine.getOptionValue("buffer-size"))
                : 1000;

        try {
            reader.inputGraph(readStream, bufferSize);
            if (!cmdLine.hasOption("skip-setting-labels")) {
                if (graph.getBaseGraph() instanceof Neo4j2Graph) {
                    // safe, due to the above instanceof
                    @SuppressWarnings("unchecked")
                    FramedGraph<Neo4j2Graph> neo4j2Graph = ((FramedGraph<Neo4j2Graph>) graph);
                    Neo4jGraphManager<?> manager = new Neo4jGraphManager<>(neo4j2Graph);
                    int i = 0;
                    for (Vertex v : graph.getVertices()) {
                        manager.setLabels(v);
                        i++;
                        if (i % 10000 == 0) {
                            neo4j2Graph.getBaseGraph().commit();
                        }
                    }
                    System.err.println("Labelled " + i + " vertices");
                }
            }
        } finally {
            readStream.close();
        }
    }
}
