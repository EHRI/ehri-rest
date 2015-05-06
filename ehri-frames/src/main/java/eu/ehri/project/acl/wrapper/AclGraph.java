package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.wrappers.WrappedGraphQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.base.Accessor;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclGraph<T extends IndexableGraph> implements WrapperGraph<T>, IndexableGraph {

    protected final T baseGraph;
    private final Accessor accessor;
    private final PipeFunction<Vertex,Boolean> aclFilter;

    public AclGraph(T graph, Accessor accessor) {
        this.baseGraph = graph;
        this.accessor = accessor;
        this.aclFilter = AclManager.getAclFilterFunction(accessor);
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
                ? (aclFilter.compute(vertex) ? vertex : null)
                : null;
    }

    @Override
    public void removeVertex(Vertex vertex) {
        baseGraph.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices()).filter(aclFilter);

    }

    @Override
    public Iterable<Vertex> getVertices(String s, Object o) {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices(s, o)).filter(aclFilter);
    }

    @Override
    public Edge addEdge(Object o, Vertex vertex, Vertex vertex2, String s) {
        return baseGraph.addEdge(o, vertex, vertex2, s);
    }

    @Override
    public Edge getEdge(Object o) {
        return baseGraph.getEdge(o);
    }

    @Override
    public void removeEdge(Edge edge) {
        baseGraph.removeEdge(edge);
    }

    @Override
    public Iterable<Edge> getEdges() {
        return baseGraph.getEdges();
    }

    @Override
    public Iterable<Edge> getEdges(String s, Object o) {
        return baseGraph.getEdges(s, o);
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

    @Override
    public <E extends Element> Index<E> createIndex(String s, Class<E> tClass, Parameter... parameters) {
        return baseGraph.createIndex(s, tClass, parameters);
    }

    @Override
    public <E extends Element> Index<E> getIndex(String s, Class<E> tClass) {
        return baseGraph.getIndex(s, tClass);
    }

    @Override
    public Iterable<Index<? extends Element>> getIndices() {
        return baseGraph.getIndices();
    }

    @Override
    public void dropIndex(String s) {
        baseGraph.dropIndex(s);
    }
}
