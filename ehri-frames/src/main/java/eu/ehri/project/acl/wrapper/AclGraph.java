package eu.ehri.project.acl.wrapper;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
public class AclGraph<T extends TransactionalGraph & IndexableGraph> implements TransactionalGraph,
        WrapperGraph<T>, IndexableGraph {

    public static interface AccessorFetcher {
        public Accessor fetch();
    }

    protected final T baseGraph;
    private final AccessorFetcher accessorFetcher;

    private final Supplier<Accessor> _supplier = new Supplier<Accessor>() {
        @Override
        public Accessor get() {
            return accessorFetcher.fetch();
        }
    };
    private volatile Supplier<Accessor> accessorSupplier = Suppliers.memoize(_supplier);
    private final Supplier<PipeFunction<Vertex,Boolean>> _afsupplier = new Supplier<PipeFunction<Vertex,Boolean>>() {
        @Override
        public PipeFunction<Vertex,Boolean> get() {
            return AclManager.getAclFilterFunction(accessorSupplier.get());
        }
    };
    private volatile Supplier<PipeFunction<Vertex,Boolean>> afsupplier = Suppliers.memoize(_afsupplier);

    public AclGraph(T graph, AccessorFetcher accessorFetcher) {
        this.baseGraph = graph;
        this.accessorFetcher = accessorFetcher;
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
                ? (getAclFilter().compute(vertex) ? vertex : null)
                : null;
    }

    @Override
    public void removeVertex(Vertex vertex) {
        baseGraph.removeVertex(vertex);
    }

    @Override
    public Iterable<Vertex> getVertices() {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices()).filter(getAclFilter());

    }

    @Override
    public Iterable<Vertex> getVertices(String s, Object o) {
        return new GremlinPipeline<Vertex, Vertex>(
                baseGraph.getVertices(s, o)).filter(getAclFilter());
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

    @Deprecated
    @Override
    public void stopTransaction(Conclusion conclusion) {
        baseGraph.stopTransaction(conclusion);
    }

    @Override
    public void shutdown() {
        baseGraph.shutdown();
    }

    @Override
    public void commit() {
        baseGraph.commit();
    }

    @Override
    public void rollback() {
        baseGraph.rollback();
    }

    @Override
    public T getBaseGraph() {
        return baseGraph;
    }

    public PipeFunction<Vertex,Boolean> getAclFilter() {
        return afsupplier.get();
    }

    public Accessor getAccessor() {
        return accessorSupplier.get();
    }

    @Override
    public <T extends Element> Index<T> createIndex(String s, Class<T> tClass, Parameter... parameters) {
        return baseGraph.createIndex(s, tClass, parameters);
    }

    @Override
    public <T extends Element> Index<T> getIndex(String s, Class<T> tClass) {
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
