package eu.ehri.project.acl.wrapper;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.wrappers.WrappedGraphQuery;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
import com.tinkerpop.pipes.PipeFunction;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclGraph<T extends TransactionalGraph & IndexableGraph> implements TransactionalGraph,
        WrapperGraph<T>, IndexableGraph {

    protected final T baseGraph;
    private final PipeFunction<Vertex,Boolean> test;

    public AclGraph(final T graph, final PipeFunction<Vertex,Boolean> test) {
        this.baseGraph = graph;
        this.test = test;
    }

    public boolean test(Vertex v) {
        return test.compute(v);
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
                    test.compute(vertex)
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
        final AclGraph<?> aclGraph = this;
        return new WrappedGraphQuery(this.baseGraph.query()) {
            @Override
            public Iterable<Edge> edges() {
                return new AclEdgeIterable(this.query.edges(), aclGraph);
            }

            @Override
            public Iterable<Vertex> vertices() {
                return new AclVertexIterable(this.query.vertices(), aclGraph);
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
