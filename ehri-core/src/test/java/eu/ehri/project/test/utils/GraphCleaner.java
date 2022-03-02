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

package eu.ehri.project.test.utils;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;

/**
 * Deletes all nodes and indices from a Neo4j graph. Use with care.
 * <p>
 * Note: This does NOT reset the Neo4j node auto-increment id.
 */
public class GraphCleaner<T extends Graph> {

    private final FramedGraph<T> graph;

    /**
     * Constructor.
     *
     * @param graph the framed graph
     */
    public GraphCleaner(FramedGraph<T> graph) {
        this.graph = graph;
    }

    /**
     * Delete all nodes and indices from the graph.
     */
    public void clean() {
        for (Vertex v : graph.getVertices()) {
            v.remove();
        }
    }
}
