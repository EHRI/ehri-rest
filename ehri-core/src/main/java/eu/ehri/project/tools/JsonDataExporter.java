/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.tools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.persistence.Bundle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Tool for exporting a graph in a manner conducive
 * to converting it to other formats, e.g. RDF triples,
 * in a streaming manner.
 */
public class JsonDataExporter {

    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Export the graph as JSON.
     *
     * @param graph  the graph database
     * @param stream the output stream
     */
    public static void outputGraph(Graph graph, OutputStream stream)
            throws IOException {

        try (JsonGenerator g = jsonFactory.createGenerator(stream)) {
            g.writeStartArray();
            for (Vertex vertex : graph.getVertices()) {

                Map<String, Object> data = Maps.newHashMap();
                data.put(Bundle.ID_KEY, vertex.getProperty(EntityType.ID_KEY));
                data.put(Bundle.TYPE_KEY, vertex.getProperty(EntityType.TYPE_KEY));
                data.put(Bundle.DATA_KEY, getProperties(vertex));
                data.put(Bundle.REL_KEY, getRelations(vertex));

                jsonMapper.writeValue(g, data);
                g.writeRaw('\n');
            }
            g.writeEndArray();
        }
    }

    private static Map<String, Object> getProperties(Vertex vertex) {
        Map<String,Object> props = Maps.newHashMap();
        for (String key : vertex.getPropertyKeys()) {
            if (!key.startsWith("_")) {
                props.put(key, vertex.getProperty(key));
            }
        }
        return props;
    }

    private static Map<String,List<List<String>>> getRelations(Vertex vertex) {
        Map<String,List<List<String>>> outRels = Maps.newHashMap();
        for (Edge e : vertex.getEdges(Direction.OUT)) {
            String label = e.getLabel();
            Vertex other = e.getVertex(Direction.IN);
            String oid = other.getProperty(EntityType.ID_KEY);
            String otype = other.getProperty(EntityType.TYPE_KEY);
            if (oid != null && otype != null) {
                List<String> ref = Lists.newArrayList(oid, otype);
                if (!outRels.containsKey(label)) {
                    outRels.put(label, Lists.<List<String>>newArrayList());
                }

                outRels.get(label).add(ref);
            }
        }
        return outRels;
    }
 }
