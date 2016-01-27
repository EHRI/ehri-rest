package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class AclVertexIterable implements CloseableIterable<Vertex> {
    private final Iterable<Vertex> iterable;
    private final AclGraph<?> aclGraph;

    public AclVertexIterable(Iterable<Vertex> iterable, AclGraph<?> aclGraph) {
        this.iterable = iterable;
        this.aclGraph = aclGraph;
    }

    @Override
    public void close() {
        if (this.iterable instanceof CloseableIterable) {
            ((CloseableIterable) iterable).close();
        }
    }

    @Override
    public Iterator<Vertex> iterator() {
        return new Iterator<Vertex>() {
            private final Iterator<Vertex> itty = iterable.iterator();
            private AclVertex nextVertex;

            @Override
            public boolean hasNext() {
                if (null != this.nextVertex) {
                    return true;
                }
                while (this.itty.hasNext()) {
                    Vertex vertex = this.itty.next();
                    if (aclGraph.evaluateVertex(vertex)) {
                        this.nextVertex = new AclVertex(vertex, aclGraph);
                        return true;
                    }
                }
                return false;

            }

            @Override
            public Vertex next() {
                if (null != this.nextVertex) {
                    AclVertex temp = this.nextVertex;
                    this.nextVertex = null;
                    return temp;
                } else {
                    while (this.itty.hasNext()) {
                        Vertex vertex = this.itty.next();
                        if (aclGraph.evaluateVertex(vertex)) {
                            return new AclVertex(vertex, aclGraph);
                        }
                    }
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
