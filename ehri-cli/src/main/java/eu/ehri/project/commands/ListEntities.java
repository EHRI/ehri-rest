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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistence.Serializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;

/**
 * Command for listing entities of a given type.
 */
public class ListEntities extends BaseCommand {

    final static String NAME = "list";

    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder("f")
                .type(String.class)
                .longOpt("full")
                .required(false)
                .hasArg(false)
                .desc("Output full JSON for items instead of just IDs")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] <type>", NAME);
    }

    @Override
    public String getHelp() {
        return "List entities of a given type.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {

        // the first argument is the entity type, and that must be specified
        if (cmdLine.getArgList().isEmpty()) {
            throw new RuntimeException(getUsage());
        }
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);
        if (cmdLine.hasOption('f')) {
            printJson(manager, serializer, type);
        } else {
            printIds(manager, type);
        }
        return 0;
    }

    private void printIds(GraphManager manager, EntityClass type) {
        try (CloseableIterable<Vertex> vertices = manager.getVertices(type)) {
            for (Vertex acc : vertices) {
                System.out.println(manager.getId(acc));
            }
        }
    }

    private void printJson(GraphManager manager, final Serializer serializer, EntityClass type)
            throws SerializationError, IOException {
        try (CloseableIterable<Vertex> vertices = manager.getVertices(type);
             JsonGenerator generator = jsonFactory.createGenerator(System.out)) {
            generator.writeStartArray();
            for (Vertex v : vertices) {
                mapper.writeValue(generator, serializer.vertexToData(v));
                generator.writeRaw('\n');
            }
            generator.writeEndArray();
        }
    }
}