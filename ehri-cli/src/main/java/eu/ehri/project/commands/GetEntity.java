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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.persistence.Serializer;
import org.apache.commons.cli.CommandLine;

/**
 * Fetch an item's serialized representation via the command line.
 */
public class GetEntity extends BaseCommand {

    final static String NAME = "get";

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] <identifier>", NAME);
    }

    @Override
    public String getHelp() {
        return "Get an entity by its identifier.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {
        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);

        if (cmdLine.getArgList().isEmpty()) {
            throw new RuntimeException(getUsage());
        }

        Vertex vertex = manager.getVertex(cmdLine.getArgs()[0]);
        System.out.println(serializer.vertexToJson(vertex));
        return 0;
    }
}
