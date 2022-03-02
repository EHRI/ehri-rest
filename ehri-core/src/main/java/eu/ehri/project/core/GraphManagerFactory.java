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

package eu.ehri.project.core;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.impl.BlueprintsGraphManager;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;

/**
 * A factory class for obtaining a {@link GraphManager} instance.
 */
public class GraphManagerFactory {

    /**
     * Obtain an instance of a graph manager.
     *
     * @param graph A transactional Blueprints graph.
     * @return A graph manager instance.
     */
    // NB: Because Java doesn't support multiple wildcard bounds we do some checking
    // of the bounds manually ourselves, which is ugly but should ensure it's safe
    // to do an unchecked cast here.
    @SuppressWarnings("unchecked")
    public static GraphManager getInstance(FramedGraph<?> graph) {
        Graph baseGraph = graph.getBaseGraph();
        if (Neo4j2Graph.class.isAssignableFrom(baseGraph.getClass())) {
            return new Neo4jGraphManager(graph);
        } else {
            return new BlueprintsGraphManager(graph);
        }
    }
}
