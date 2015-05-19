package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclVertexIterable implements CloseableIterable<Vertex> {
    private final Iterable<Vertex> iterable;
    private final AclGraph<?> graph;
    private final PipeFunction<Vertex,Boolean> aclFilter;

    public AclVertexIterable(final Iterable<Vertex> iterable, final AclGraph<?> graph) {
        this.iterable = iterable;
        this.graph = graph;
        this.aclFilter = AclManager.getAclFilterFunction(graph.getAccessor());
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
                    final Vertex vertex = this.itty.next();
                    if (aclFilter.compute(vertex)) {
                        this.nextVertex = new AclVertex(vertex, graph);
                        return true;
                    }
                }
                return false;

            }

            @Override
            public Vertex next() {
                if (null != this.nextVertex) {
                    final AclVertex temp = this.nextVertex;
                    this.nextVertex = null;
                    return temp;
                } else {
                    while (this.itty.hasNext()) {
                        final Vertex vertex = this.itty.next();
                        if (aclFilter.compute(vertex)) {
                            return new AclVertex(vertex, graph);
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
