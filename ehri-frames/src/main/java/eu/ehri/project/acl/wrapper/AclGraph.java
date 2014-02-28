package eu.ehri.project.acl.wrapper;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.wrappers.WrappedGraphQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.base.Accessor;

import java.util.List;

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
        return new AclVertex(baseGraph.addVertex(o), this);
    }

    @Override
    public Vertex getVertex(Object o) {
        Vertex vertex = baseGraph.getVertex(o);
        return vertex != null
                ? (
                    getAclFilter().compute(vertex)
                            ? new AclVertex(vertex, this)
                            : null)
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
        Vertex v1 = (vertex instanceof AclVertex) ? ((AclVertex) vertex).getBaseVertex() : vertex;
        Vertex v2 = (vertex2 instanceof AclVertex) ? ((AclVertex) vertex2).getBaseVertex() : vertex2;
        return new AclEdge(baseGraph.addEdge(o, v1, v2, s), this);
    }

    @Override
    public Edge getEdge(Object o) {
        return new AclEdge(baseGraph.getEdge(o), this);
    }

    @Override
    public void removeEdge(Edge edge) {
        Edge e = (edge instanceof AclEdge) ? ((AclEdge) edge).getBaseEdge() : edge;
        baseGraph.removeEdge(e);
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
        return new AclIndex<T>(baseGraph.createIndex(s, tClass, parameters), this);
    }

    @Override
    public <T extends Element> Index<T> getIndex(String s, Class<T> tClass) {
        return new AclIndex<T>(baseGraph.getIndex(s, tClass), this);
    }

    @Override
    public Iterable<Index<? extends Element>> getIndices() {
        List<Index<? extends Element>> indices = Lists.newArrayList();
        for (Index<?> index : baseGraph.getIndices()) {
            indices.add(new AclIndex(index, this));
        }
        return indices;
    }

    @Override
    public void dropIndex(String s) {
        baseGraph.dropIndex(s);
    }
}
