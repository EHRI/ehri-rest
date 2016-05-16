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

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Add an arbitrary edge between two nodes.
 */
public class RelationAdd extends BaseCommand {

    final static String NAME = "add-rel";

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder("s")
                .longOpt("single")
                .desc("Ensure the out entity only has one relationship of this type by removing any others")
                .build());
        options.addOption(Option.builder("d")
                .longOpt("allow-duplicates")
                .desc("Allow creating multiple edges with the same label between the same two nodes")
                .build());
    }

    @Override
    public String getUsage() {
        return "Usage: add-rel [OPTIONS] <source> <rel-name> <target>";
    }

    @Override
    public String getHelp() {
        return "Create a relationship between a source and a target";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound {

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        if (cmdLine.getArgList().size() < 3)
            throw new RuntimeException(getUsage());

        String src = cmdLine.getArgList().get(0);
        String label = cmdLine.getArgList().get(1);
        String dst = cmdLine.getArgList().get(2);

        Vertex source = manager.getVertex(src);
        Vertex target = manager.getVertex(dst);

        if (cmdLine.hasOption("allow-duplicates")) {
            source.addEdge(label, target);
        } else if (cmdLine.hasOption("unique")) {
            if (!JavaHandlerUtils.addUniqueRelationship(source, target, label)) {
                System.err.println("Relationship already exists");
            }
        } else {
            if (!JavaHandlerUtils.addSingleRelationship(source, target, label)) {
                System.err.println("Relationship already exists");
            }
        }

        return 0;
    }
}
