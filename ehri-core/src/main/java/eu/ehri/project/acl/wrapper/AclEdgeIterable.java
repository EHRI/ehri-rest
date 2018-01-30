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
            private Iterator<Edge> itty = iterable.iterator();
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
