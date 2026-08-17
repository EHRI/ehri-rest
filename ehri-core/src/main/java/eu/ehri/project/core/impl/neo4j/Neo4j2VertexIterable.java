/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.core.impl.neo4j;


import com.tinkerpop.blueprints.CloseableIterable;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Iterator;


public class Neo4j2VertexIterable<T extends Neo4j2Vertex> implements CloseableIterable<Neo4j2Vertex> {

    private final ResourceIterable<Node> nodes;
    private final Neo4j2Graph graph;

    public Neo4j2VertexIterable(ResourceIterable<Node> nodes, Neo4j2Graph graph) {
        this.nodes = nodes;
        this.graph = graph;
    }

    public Iterator<Neo4j2Vertex> iterator() {
        graph.autoStartTransaction(false);
        return new Iterator<Neo4j2Vertex>() {
            private final Iterator<Node> itty = nodes.iterator();

            public void remove() {
                this.itty.remove();
            }

            public Neo4j2Vertex next() {
                graph.autoStartTransaction(false);
                return new Neo4j2Vertex(this.itty.next(), graph);
            }

            public boolean hasNext() {
                graph.autoStartTransaction(false);
                return this.itty.hasNext();
            }
        };
    }

    public void close() {
        if (this.nodes instanceof IndexHits) {
            ((IndexHits<?>) this.nodes).close();
        }
    }

}