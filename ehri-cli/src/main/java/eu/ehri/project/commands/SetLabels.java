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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.models.annotations.EntityType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.*;

import static eu.ehri.project.core.impl.Neo4jGraphManager.BASE_LABEL;

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
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder("b")
                .hasArg().type(Integer.class)
                .longOpt("buffer-size")
                .desc("Transaction buffer size").build());
    }

    public static void labelNodes(GraphDatabaseService service, int batchSize, Label baseLabel) {
        long processedCount = 0;
        boolean hasMore = true;

        while (hasMore) {
            try (Transaction tx = service.beginTx()) {
                // Query that uses SKIP/LIMIT for pagination
                String query = "MATCH (n) RETURN n SKIP " + processedCount + " " + "LIMIT " + batchSize;
                ResourceIterator<Node> nodes = tx.execute(query).columnAs("n");

                int batchCount = 0;
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    node.addLabel(baseLabel);
                    node.addLabel(Label.label((String) node.getProperty(EntityType.TYPE_KEY)));
                    batchCount++;
                }

                // If we processed fewer nodes than the batch size, we're done
                hasMore = (batchCount == batchSize);
                processedCount += batchCount;

                tx.commit();
                System.out.println("Processed " + processedCount + " nodes");
            }
        }
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {
        int bufferSize = cmdLine.hasOption("buffer-size")
                ? Integer.parseInt(cmdLine.getOptionValue("buffer-size"))
                : 1000;
        Graph baseGraph = graph.getBaseGraph();
        if (baseGraph instanceof Neo4j2Graph) {
            GraphDatabaseService service = ((Neo4j2Graph) baseGraph).getRawGraph();
            Label baseLabel = Label.label(BASE_LABEL);
            labelNodes(service, bufferSize, baseLabel);
        } else {
            System.err.println("ERROR: Cannot set labels on non Neo4j2-graph");
        }
        return 0;
    }
}
