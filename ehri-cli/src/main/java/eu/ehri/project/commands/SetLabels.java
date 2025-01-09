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

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import org.apache.commons.cli.CommandLine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * Command for generating the (Neo4j) graph schema.
 */
public class SetLabels extends BaseCommand {

    final static String NAME = "set-labels";

    @Override
    public String getUsage() {
        return NAME;
    }

    @Override
    public String getHelp() {
        return "Set node labels to match types per the __type property.";
    }


    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {
        Graph baseGraph = graph.getBaseGraph();
        if (baseGraph instanceof Neo4j2Graph) {
            // safe, due to the above instanceof
            @SuppressWarnings("unchecked")
            FramedGraph<Neo4j2Graph> neo4j2Graph = (FramedGraph<Neo4j2Graph>) graph;
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
        } else {
            System.err.println("ERROR: Cannot generate schema on a non-Neo4j2 graph");
        }
        return 0;
    }
}
