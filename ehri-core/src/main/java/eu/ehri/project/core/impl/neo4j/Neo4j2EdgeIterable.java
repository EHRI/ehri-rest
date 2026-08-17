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
import com.tinkerpop.blueprints.Edge;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Iterator;


public class Neo4j2EdgeIterable<T extends Edge> implements CloseableIterable<Neo4j2Edge> {

    private final Iterable<Relationship> relationships;
    private final Neo4j2Graph graph;

    @Deprecated
    public Neo4j2EdgeIterable(Iterable<Relationship> relationships, Neo4j2Graph graph, boolean checkTransaction) {
        this(relationships, graph);
    }

    public Neo4j2EdgeIterable(Iterable<Relationship> relationships, Neo4j2Graph graph) {
        this.relationships = relationships;
        this.graph = graph;
    }

    public Iterator<Neo4j2Edge> iterator() {
        graph.autoStartTransaction(true);
        return new Iterator<Neo4j2Edge>() {
            private final Iterator<Relationship> itty = relationships.iterator();

            public void remove() {
                graph.autoStartTransaction(true);
                this.itty.remove();
            }

            public Neo4j2Edge next() {
                graph.autoStartTransaction(false);
                return new Neo4j2Edge(this.itty.next(), graph);
            }

            public boolean hasNext() {
                graph.autoStartTransaction(false);
                return this.itty.hasNext();
            }
        };
    }

    public void close() {
        if (this.relationships instanceof IndexHits) {
            ((IndexHits<?>) this.relationships).close();
        }
    }
}