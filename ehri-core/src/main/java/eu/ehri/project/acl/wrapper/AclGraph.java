package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.wrappers.WrappedGraphQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
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
    private final PipeFunction<Vertex,Boolean> aclVertexFilter;
    private final PipeFunction<Edge,Boolean> aclEdgeFilter;

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
                ? (aclVertexFilter.compute(vertex) ? vertex : null)
                : null;
    }

    @Override
    public void removeVertex(Vertex vertex) {
        baseGraph.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices()).filter(aclVertexFilter);

    }

    @Override
    public Iterable<Vertex> getVertices(String s, Object o) {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices(s, o)).filter(aclVertexFilter);
    }

    @Override
    public Edge addEdge(Object o, Vertex vertex, Vertex vertex2, String s) {
        return baseGraph.addEdge(o, vertex, vertex2, s);
    }

    @Override
    public Edge getEdge(Object o) {
        Edge edge = baseGraph.getEdge(o);
        return edge != null
                ? (aclEdgeFilter.compute(edge) ? edge : null)
                : null;
    }

    @Override
    public void removeEdge(Edge edge) {
        baseGraph.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return new GremlinPipeline<Edge, Edge>(
                baseGraph.getEdges()).filter(aclEdgeFilter);
    }

    @Override
    public Iterable<Edge> getEdges(String s, Object o) {
        return new GremlinPipeline<Edge, Edge>(
                baseGraph.getEdges(s, o)).filter(aclEdgeFilter);
    }

    @Override
    public GraphQuery query() {
        final AclGraph<?> partitionGraph = this;
        return new WrappedGraphQuery(this.baseGraph.query()) {
            @Override
            public Iterable<Edge> edges() {
                return new AclEdgeIterable(this.query.edges(), partitionGraph);
            }

            @Override
            public Iterable<Vertex> vertices() {
                return new AclVertexIterable(this.query.vertices(), partitionGraph);
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
}
