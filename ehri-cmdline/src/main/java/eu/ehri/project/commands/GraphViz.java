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

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Dump a particular subgraph as a DOT file.
 *
 */
public class GraphViz extends BaseCommand implements Command {

    final static String NAME = "graphviz";

    public GraphViz() {
    }

    @Override
    public String getHelp() {
        return "Usage: graphviz [OPTIONS] <type> <identifier>";
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("r", "relationship", true,
                "A relationship to include in the graph"));
    }

    @Override
    public String getUsage() {
        return "Dump a dot file.";
    }

    @Override
    @SuppressWarnings("unchecked")
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception {

        final GraphManager manager = GraphManagerFactory.getInstance(graph);
        GraphDatabaseService neo4jGraph = ((Neo4jGraph)graph.getBaseGraph()).getRawGraph();

        // Cmdline arguments should be a node and a list of relationship types
        // to traverse.
        if (cmdLine.getArgList().size() < 2)
            throw new RuntimeException(getHelp());

        List<Node> nodes = Lists.newArrayList();
        for (int i = 0; i < cmdLine.getArgs().length; i++) {
            nodes.add(neo4jGraph.getNodeById(
                    (Long)manager.getVertex(cmdLine.getArgs()[i]).getId()));
        }

        if (!cmdLine.hasOption("relationship")) {
            throw new RuntimeException("No --relationship arguments given");
        }

        DynamicRelationshipType[] rels = new DynamicRelationshipType[
                cmdLine.getOptionValues("relationship").length];
        if (cmdLine.hasOption("relationship")) {
            int i = 0;
            for (String rel : cmdLine.getOptionValues("relationship")) {
                rels[i++] = DynamicRelationshipType.withName(rel);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GraphvizWriter writer = new GraphvizWriter();

        writer.emit(out, Walker.crosscut(nodes, rels));
        out.writeTo(System.out);
        return 0;
    }
}
