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

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class AclEdgeIterable implements CloseableIterable<Edge> {

    private final Iterable<Edge> iterable;
    private final AclGraph<?> aclGraph;

    public AclEdgeIterable(Iterable<Edge> iterable,
            AclGraph<?> aclGraph) {
        this.iterable = iterable;
        this.aclGraph = aclGraph;
    }

    @Override
    public void close() {
        if (this.iterable instanceof CloseableIterable<?>) {
            ((CloseableIterable<?>) iterable).close();
        }
    }

    @Override
    public Iterator<Edge> iterator() {
        return new Iterator<Edge>() {
            private final Iterator<Edge> itty = iterable.iterator();
            private AclEdge nextEdge;

            public void remove() {
                this.itty.remove();
            }

            public boolean hasNext() {
                if (null != this.nextEdge) {
                    return true;
                }
                while (this.itty.hasNext()) {
                    Edge edge = this.itty.next();
                    if (aclGraph.evaluateEdge(edge)) {
                        nextEdge = new AclEdge(edge, aclGraph);
                        return true;
                    }
                }
                return false;

            }

            public Edge next() {
                if (null != this.nextEdge) {
                    AclEdge temp = this.nextEdge;
                    this.nextEdge = null;
                    return temp;
                } else {
                    while (this.itty.hasNext()) {
                        Edge edge = this.itty.next();
                        if (aclGraph.evaluateEdge(edge)) {
                            return new AclEdge(edge, aclGraph);
                        }
                    }
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
