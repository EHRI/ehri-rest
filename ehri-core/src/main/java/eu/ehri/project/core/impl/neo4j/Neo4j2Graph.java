package eu.ehri.project.core.impl.neo4j;

import com.google.common.base.Preconditions;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.MetaGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.DefaultGraphQuery;
import com.tinkerpop.blueprints.util.ExceptionFactory;
import com.tinkerpop.blueprints.util.PropertyFilteredIterable;
import com.tinkerpop.blueprints.util.StringFactory;
import com.tinkerpop.blueprints.util.WrappingCloseableIterable;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A Blueprints implementation of the graph database Neo4j (http://neo4j.org)
 */
public class Neo4j2Graph implements TransactionalGraph, MetaGraph<GraphDatabaseService> {
    private static final Logger logger = Logger.getLogger(Neo4j2Graph.class.getName());

    private GraphDatabaseService rawGraph;

    protected final ThreadLocal<Transaction> tx = new ThreadLocal<Transaction>() {
        protected Transaction initialValue() {
            return null;
        }
    };

    protected final ThreadLocal<Boolean> checkElementsInTransaction = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    private static final Features FEATURES = new Features();

    static {

        FEATURES.supportsSerializableObjectProperty = false;
        FEATURES.supportsBooleanProperty = true;
        FEATURES.supportsDoubleProperty = true;
        FEATURES.supportsFloatProperty = true;
        FEATURES.supportsIntegerProperty = true;
        FEATURES.supportsPrimitiveArrayProperty = true;
        FEATURES.supportsUniformListProperty = true;
        FEATURES.supportsMixedListProperty = false;
        FEATURES.supportsLongProperty = true;
        FEATURES.supportsMapProperty = false;
        FEATURES.supportsStringProperty = true;

        FEATURES.supportsDuplicateEdges = true;
        FEATURES.supportsSelfLoops = true;
        FEATURES.isPersistent = true;
        FEATURES.isWrapper = false;
        FEATURES.supportsVertexIteration = true;
        FEATURES.supportsEdgeIteration = true;
        FEATURES.supportsVertexIndex = false;
        FEATURES.supportsEdgeIndex = true;
        FEATURES.ignoresSuppliedIds = true;
        FEATURES.supportsTransactions = true;
        FEATURES.supportsIndices = true;
        FEATURES.supportsKeyIndices = true;
        FEATURES.supportsVertexKeyIndex = false;
        FEATURES.supportsEdgeKeyIndex = false;
        FEATURES.supportsEdgeRetrieval = true;
        FEATURES.supportsVertexProperties = true;
        FEATURES.supportsEdgeProperties = true;
        FEATURES.supportsThreadedTransactions = false;
        FEATURES.supportsThreadIsolatedTransactions = true;
    }

    protected boolean checkElementsInTransaction() {
        if (this.tx.get() == null) {
            return false;
        } else {
            return this.checkElementsInTransaction.get();
        }
    }

    public Neo4j2Graph(String directory) {
        this(directory, null);
    }

    public Neo4j2Graph(GraphDatabaseService rawGraph) {
        this.rawGraph = rawGraph;

        init();
    }

    public Neo4j2Graph(String directory, Map<String, String> configuration) {
        try {
            GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(directory));
            if (null != configuration)
                this.rawGraph = builder.setConfig(configuration).newGraphDatabase();
            else
                this.rawGraph = builder.newGraphDatabase();

            init();

        } catch (Exception e) {
            if (this.rawGraph != null)
                this.rawGraph.shutdown();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void init() {
    }

    public Neo4j2Graph(Configuration configuration) {
        this(configuration.getString("blueprints.neo4j.directory", null),
                ConfigurationConverter.getMap(configuration.subset("blueprints.neo4j.conf")));
    }

    @Override
    public Neo4j2Vertex addVertex(Object id) {
        this.autoStartTransaction(true);
        return new Neo4j2Vertex(this.rawGraph.createNode(), this);
    }

    @Override
    public Neo4j2Vertex getVertex(Object id) {
        this.autoStartTransaction(false);

        if (null == id)
            throw ExceptionFactory.vertexIdCanNotBeNull();

        try {
            long longId;
            if (id instanceof Long)
                longId = (Long) id;
            else if (id instanceof Number)
                longId = ((Number) id).longValue();
            else
                longId = Double.valueOf(id.toString()).longValue();
            return new Neo4j2Vertex(this.rawGraph.getNodeById(longId), this);
        } catch (NotFoundException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The underlying Neo4j graph does not natively support this method within a
     * transaction. If the graph is not currently in a transaction, then the
     * operation runs efficiently and correctly. If the graph is currently in a
     * transaction, please use setCheckElementsInTransaction() if it is
     * necessary to ensure proper transactional semantics. Note that it is
     * costly to check if an element is in the transaction.
     *
     * @return all the vertices in the graph
     */
    @Override
    public CloseableIterable<Vertex> getVertices() {
        this.autoStartTransaction(false);
        return new Neo4j2VertexIterable(rawGraph.getAllNodes(), this);
    }

    public CloseableIterable<Vertex> getVerticesByLabel(final String label) {
        this.autoStartTransaction(false);
        ResourceIterable<Node> wrap = () -> rawGraph.findNodes(Label.label(label));
        return new Neo4j2VertexIterable(wrap, this);
    }

    public CloseableIterable<Vertex> getVerticesByLabelKeyValue(
            final String label, final String key, final Object value) {
        ResourceIterable<Node> wrap = () -> {
            autoStartTransaction(false);
            return rawGraph.findNodes(Label.label(label), key, value);
        };
        return new Neo4j2VertexIterable(wrap, this);
    }

    @Override
    public CloseableIterable<Vertex> getVertices(String key, Object value) {
        this.autoStartTransaction(false);
        return new PropertyFilteredIterable<>(key, value, this.getVertices());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The underlying Neo4j graph does not natively support this method within a
     * transaction. If the graph is not currently in a transaction, then the
     * operation runs efficiently and correctly. If the graph is currently in a
     * transaction, please use setCheckElementsInTransaction() if it is
     * necessary to ensure proper transactional semantics. Note that it is
     * costly to check if an element is in the transaction.
     *
     * @return all the edges in the graph
     */
    @Override
    public CloseableIterable<Edge> getEdges() {
        this.autoStartTransaction(false);
        return new Neo4j2EdgeIterable(rawGraph.getAllRelationships(), this);
    }

    @Override
    public Iterable<Edge> getEdges(String key, Object value) {
        this.autoStartTransaction(false);
        return new PropertyFilteredIterable<>(key, value, this.getEdges());
    }

    @Override
    public void removeVertex(Vertex vertex) {
        this.autoStartTransaction(true);

        try {
            Node node = ((Neo4j2Vertex) vertex).getRawVertex();
            for (Relationship relationship : node.getRelationships(org.neo4j.graphdb.Direction.BOTH)) {
                relationship.delete();
            }
            node.delete();
        } catch (NotFoundException | IllegalStateException nfe) {
            throw ExceptionFactory.vertexWithIdDoesNotExist(vertex.getId());
        }
    }

    @Override
    public Neo4j2Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
        if (label == null)
            throw ExceptionFactory.edgeLabelCanNotBeNull();

        this.autoStartTransaction(true);
        return new Neo4j2Edge(((Neo4j2Vertex) outVertex).getRawVertex().createRelationshipTo(((Neo4j2Vertex) inVertex).getRawVertex(),
                RelationshipType.withName(label)), this);
    }

    @Override
    public Neo4j2Edge getEdge(Object id) {
        if (null == id)
            throw ExceptionFactory.edgeIdCanNotBeNull();

        this.autoStartTransaction(true);
        try {
            Long longId;
            if (id instanceof Long)
                longId = (Long) id;
            else
                longId = Double.valueOf(id.toString()).longValue();
            return new Neo4j2Edge(this.rawGraph.getRelationshipById(longId), this);
        } catch (NotFoundException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void removeEdge(Edge edge) {
        this.autoStartTransaction(true);
        ((Relationship) ((Neo4j2Edge) edge).getRawElement()).delete();
    }

    @Override
    public void stopTransaction(Conclusion conclusion) {
        if (Conclusion.SUCCESS == conclusion)
            commit();
        else
            rollback();
    }

    @Override
    public void commit() {
        if (null == tx.get()) {
            return;
        }

        try {
            tx.get().success();
        } finally {
            tx.get().close();
            tx.remove();
        }
    }

    @Override
    public void rollback() {
        if (null == tx.get()) {
            return;
        }

        try {
            tx.get().failure();
        } finally {
            tx.get().close();
            tx.remove();
        }
    }

    @Override
    public void shutdown() {
        try {
            this.commit();
        } catch (TransactionFailureException e) {
            logger.warning("Failure on shutdown " + e.getMessage());
            // TODO: inspect why certain transactions fail
        }
        this.rawGraph.shutdown();
    }

    // The forWrite flag is true when the autoStartTransaction method is
    // called before any operation which will modify the graph in any way. It
    // is not used in this simple implementation but is required in subclasses
    // which enforce transaction rules. Now that Neo4j reads also require a
    // transaction to be open it is otherwise impossible to tell the difference
    // between the beginning of a write operation and the beginning of a read
    // operation.
    public void autoStartTransaction(boolean forWrite) {
        if (tx.get() == null) {
            tx.set(this.rawGraph.beginTx());
        }
    }

    public GraphDatabaseService getRawGraph() {
        return this.rawGraph;
    }

    public Features getFeatures() {
        return FEATURES;
    }

    public String toString() {
        return StringFactory.graphString(this, this.rawGraph.toString());
    }

    public GraphQuery query() {
        return new DefaultGraphQuery(this);
    }

    public CloseableIterable<Map<String, Object>> query(String query, Map<String, Object> params) {
        ResourceIterable<Map<String, Object>> wrap = () -> {
            autoStartTransaction(false);
            return rawGraph.execute(query, params == null ? Collections.emptyMap() : params);
        };
        return new WrappingCloseableIterable<>(wrap);
    }
}
