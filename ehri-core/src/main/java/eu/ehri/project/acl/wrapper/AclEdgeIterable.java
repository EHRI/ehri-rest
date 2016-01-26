package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class AclEdgeIterable implements CloseableIterable<Edge> {

    private final Iterable<Edge> iterable;
    private final AclGraph<?> graph;
    private final PipeFunction<Vertex, Boolean> aclVertexFilter;
    private final PipeFunction<Edge, Boolean> aclEdgeFilter;

    public AclEdgeIterable(Iterable<Edge> iterable, AclGraph<?> graph) {
        this.iterable = iterable;
        this.graph = graph;
        this.aclVertexFilter = AclManager.getAclFilterFunction(graph.getAccessor());
        this.aclEdgeFilter = new PipeFunction<Edge, Boolean>() {
            @Override
            public Boolean compute(Edge edge) {
                return aclVertexFilter.compute(edge.getVertex(Direction.OUT))
                        && aclVertexFilter.compute(edge.getVertex(Direction.IN));
            }
        };
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
                    Edge edge = this.itty.next();
                    if (aclEdgeFilter.compute(edge)) {
                        nextEdge = new AclEdge(edge, graph);
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
                        if (aclEdgeFilter.compute(edge)) {
                            return new AclEdge(edge, graph);
                        }
                    }
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
