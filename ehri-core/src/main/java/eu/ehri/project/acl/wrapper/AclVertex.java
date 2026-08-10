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

package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperVertexQuery;


public class AclVertex extends AclElement implements Vertex {

    protected AclVertex(Vertex baseVertex, AclGraph<?> aclGraph) {
        super(baseVertex, aclGraph);
    }

    @Override
    public Iterable<Edge> getEdges(Direction direction, String... strings) {
        return new AclEdgeIterable(((Vertex) this.baseElement).getEdges(direction, strings), aclGraph);
    }

    @Override
    public Iterable<Vertex> getVertices(Direction direction, String... strings) {
        return new AclVertexIterable(((Vertex) this.baseElement).getVertices(direction, strings), aclGraph);
    }

    @Override
    public VertexQuery query() {
        return new WrapperVertexQuery(((Vertex) this.baseElement).query()) {
            @Override
            public Iterable<Vertex> vertices() {
                return new AclVertexIterable(this.query.vertices(), aclGraph);
            }

            @Override
            public Iterable<Edge> edges() {
                return this.query.edges();
            }
        };
    }

    @Override
    public Edge addEdge(String label, Vertex vertex) {
        return aclGraph.addEdge(null, this, vertex, label);
    }

    public Vertex getBaseVertex() {
        return (Vertex) this.baseElement;
    }

    @Override
    public String toString() {
        return "aclvertex(" + aclGraph.getAccessor().getId() + ")[" + getBaseVertex() + "]";
    }
}
