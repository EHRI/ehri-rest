package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclEdgeIterable implements CloseableIterable<Edge> {

    private final Iterable<Edge> iterable;
    private final AclGraph<?> graph;

    public AclEdgeIterable(final Iterable<Edge> iterable, final AclGraph<?> graph) {
        this.iterable = iterable;
        this.graph = graph;
    }


    @Override
    public void close() {
        if (this.iterable instanceof CloseableIterable) {
            ((CloseableIterable) iterable).close();
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
                    final Edge edge = this.itty.next();
                    nextEdge = new AclEdge(edge, graph);
                    return true;
                }
                return false;

            }

            public Edge next() {
                if (null != this.nextEdge) {
                    final AclEdge temp = this.nextEdge;
                    this.nextEdge = null;
                    return temp;
                } else {
                    while (this.itty.hasNext()) {
                        final Edge edge = this.itty.next();
                        return new AclEdge(edge, graph);
                    }
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
