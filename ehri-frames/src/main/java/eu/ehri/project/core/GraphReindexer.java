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

package eu.ehri.project.core;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;

/**
 * Reindex the internal graph index.
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 */
public class GraphReindexer {

    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;

    public GraphReindexer(FramedGraph<? extends TransactionalGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * recreate the index for all the Entity vertices
     */
    public void reindex() {
        // clear the index
        try {
            manager.rebuildIndex();
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
        }
    }
}