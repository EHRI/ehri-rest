package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.WrappedGraphQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.base.Accessor;

/**
 * A wrapper graph that hides vertices which are not accessible to the
 * given accessor.
 *
 * @param <T> the underlying graph
 */
public class AclGraph<T extends Graph> implements WrapperGraph<T>, Graph {

    protected final T baseGraph;
    private final Accessor accessor;
    private final PipeFunction<Vertex, Boolean> aclVertexFilter;
    private final PipeFunction<Edge, Boolean> aclEdgeFilter;

    public AclGraph(T graph, Accessor accessor) {
        this.baseGraph = graph;
        this.accessor = accessor;
        this.aclVertexFilter = AclManager.getAclFilterFunction(accessor);
        this.aclEdgeFilter = new PipeFunction<Edge, Boolean>() {
            @Override
            public Boolean compute(Edge edge) {
                return aclVertexFilter.compute(edge.getVertex(Direction.OUT))
                        && aclVertexFilter.compute(edge.getVertex(Direction.IN));
            }
        };
    }

    @Override
    public Features getFeatures() {
        return baseGraph.getFeatures();
    }

    @Override
    public Vertex addVertex(Object o) {
        return baseGraph.addVertex(o);
    }

    @Override
    public Vertex getVertex(Object o) {
        Vertex vertex = baseGraph.getVertex(o);
        return vertex != null
                ? (aclVertexFilter.compute(vertex) ? new AclVertex(vertex, this) : null)
                : null;
    }

    @Override
    public void removeVertex(Vertex vertex) {
        baseGraph.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return new AclVertexIterable(baseGraph.getVertices(), this);

    }

    @Override
    public Iterable<Vertex> getVertices(String s, Object o) {
        return new AclVertexIterable(baseGraph.getVertices(s, o), this);
    }

    @Override
    public Edge addEdge(Object o, Vertex vertex, Vertex vertex2, String s) {
        return baseGraph.addEdge(o, vertex, vertex2, s);
    }

    @Override
    public Edge getEdge(Object o) {
        Edge edge = baseGraph.getEdge(o);
        return edge != null
                ? (aclEdgeFilter.compute(edge) ? new AclEdge(edge, this) : null)
                : null;
    }

    @Override
    public void removeEdge(Edge edge) {
        baseGraph.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return new AclEdgeIterable(baseGraph.getEdges(), this);
    }

    @Override
    public Iterable<Edge> getEdges(String s, Object o) {
        return new AclEdgeIterable(baseGraph.getEdges(s, o), this);
    }

    @Override
    public GraphQuery query() {
        final AclGraph<?> graph = this;
        return new WrappedGraphQuery(this.baseGraph.query()) {
            @Override
            public Iterable<Edge> edges() {
                return new AclEdgeIterable(this.query.edges(), graph);
            }

            @Override
            public Iterable<Vertex> vertices() {
                return new AclVertexIterable(this.query.vertices(), graph);
            }
        };
    }

    @Override
    public void shutdown() {
        baseGraph.shutdown();
    }

    @Override
    public T getBaseGraph() {
        return baseGraph;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    @Override
    public String toString() {
        return "aclgraph(" + accessor.getId() + ")[" + baseGraph + "]";
    }

    public boolean evaluateVertex(Vertex vertex) {
        return aclVertexFilter.compute(vertex);
    }

    public boolean evaluateEdge(Edge edge) {
        return aclEdgeFilter.compute(edge);
    }

    /**
     * Get an ACL-aware vertex from a generic one.
     *
     * @param vertex a vertex
     * @return a vertex with ACL behaviour
     */
    public AclVertex aclVertex(Vertex vertex) {
        return new AclVertex(vertex, this);
    }
}
