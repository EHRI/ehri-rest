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


import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.ExceptionFactory;
import com.tinkerpop.blueprints.util.StringFactory;
import org.neo4j.graphdb.Relationship;


public class Neo4j2Edge extends Neo4j2Element implements Edge {

    public Neo4j2Edge(Relationship relationship, Neo4j2Graph graph) {
        super(graph);
        this.rawElement = relationship;
    }

    public String getLabel() {
        this.graph.autoStartTransaction(false);
        return ((Relationship) this.rawElement).getType().name();
    }

    public Vertex getVertex(Direction direction) {
        this.graph.autoStartTransaction(false);
        if (direction.equals(Direction.OUT))
            return new Neo4j2Vertex(((Relationship) this.rawElement).getStartNode(), this.graph);
        else if (direction.equals(Direction.IN))
            return new Neo4j2Vertex(((Relationship) this.rawElement).getEndNode(), this.graph);
        else
            throw ExceptionFactory.bothIsNotSupported();

    }

    public boolean equals(Object object) {
        return object instanceof Neo4j2Edge && ((Neo4j2Edge) object).getId().equals(this.getId());
    }

    public String toString() {
        return StringFactory.edgeString(this);
    }

    public Relationship getRawEdge() {
        return (Relationship) this.rawElement;
    }
}
