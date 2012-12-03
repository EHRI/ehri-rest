package eu.ehri.project.core.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertexIterable;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.models.annotations.EntityType;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes.
 * 
 * @author mike
 * 
 */
public final class SingleIndexGraphManager implements GraphManager {

    public static final String INDEX_NAME = "entities";

    private final FramedGraph<Neo4jGraph> graph;

    /**
     * Constructor.
     * 
     * @param graph
     */
    public SingleIndexGraphManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
    }

    // Access functions
    public boolean exists(String id) {
        return getIndex().count(EntityType.ID_KEY, id) > 0L;
    }

    public <T> T getFrame(String id, Class<T> cls) {
        return graph.frame(getVertex(id), cls);
    }
    
    public <T> T getFrame(String id, String type, Class<T> cls) {
        return graph.frame(getVertex(id, type), cls);
    }
    
    public <T> Iterable<T> getFrames(String type, Class<T> cls) {
        CloseableIterable<Vertex> vertices = getVertices(type);
        try {
            return graph.frameVertices(getVertices(type), cls);
        } finally {
            vertices.close();
        }
    }

    public CloseableIterable<Vertex> getVertices(String type) {
        return getIndex().get(EntityType.TYPE_KEY, type);
    }

    @SuppressWarnings("unchecked")
    public CloseableIterable<Neo4jVertex> getVertices(String key, Object value,
            String type) {
        String queryStr = getLuceneQuery(key, value, type);
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return new Neo4jVertexIterable<Vertex>(rawQuery, graph.getBaseGraph(),
                false);
    }

    public Vertex getVertex(String id) {
        if (id == null)
            throw new IllegalArgumentException(
                    "Attempting to fetch vertex with null id");
        CloseableIterable<Vertex> query = getIndex().get(EntityType.ID_KEY, id);
        try {
            return query.iterator().next();
        } finally {
            query.close();
        }
    }
    
    public Vertex getVertex(String id, String type) {
        String queryStr = getLuceneQuery(EntityType.ID_KEY, id, type);
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return new Neo4jVertex(rawQuery.getSingle(), graph.getBaseGraph());
    }

    /**
     * Create a vertex with no unique keys, indexing all items.
     * 
     * @param id
     * @param data
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, String type, Map<String, Object> data)
            throws IntegrityError {
        return createVertex(id, type, data, data.keySet(),
                new LinkedList<String>());
    }

    /**
     * Create a vertex with no unique keys, indexing only the given items.
     * 
     * @param id
     * @param data
     * @param keys
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, String type,
            Map<String, Object> data, List<String> keys) throws IntegrityError {
        return createVertex(id, type, data, keys, new LinkedList<String>());
    }

    /**
     * Create a vertex with specific unique keys, indexing the given key set.
     * 
     * @param id
     * @param data
     * @param keys
     * @param uniqueKeys
     * @return
     * @throws IntegrityError
     */
    public Vertex createVertex(String id, String type,
            Map<String, Object> data, Collection<String> keys,
            Collection<String> uniqueKeys) throws IntegrityError {
        Index<Vertex> index = getIndex();
        Map<String, Object> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(id, type, keys);
        try {
            checkUniqueness(index, uniqueKeys, data, null);
            checkExists(index, id);
            Vertex node = graph.addVertex(null);
            for (Map.Entry<String, Object> entry : indexData.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                node.setProperty(entry.getKey(), entry.getValue());
                if (keys == null || indexKeys.contains(entry.getKey())) {
                    index.put(entry.getKey(), String.valueOf(entry.getValue()),
                            node);
                }
            }
            graph.getBaseGraph().stopTransaction(
                    TransactionalGraph.Conclusion.SUCCESS);
            return node;
        } catch (IntegrityError e) {
            graph.getBaseGraph().stopTransaction(
                    TransactionalGraph.Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(
                    TransactionalGraph.Conclusion.FAILURE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Vertex updateVertex(String id, String type, Map<String, Object> data)
            throws IntegrityError {
        return updateVertex(id, type, data, data.keySet(),
                new LinkedList<String>());
    }

    public Vertex updateVertex(String id, String type,
            Map<String, Object> data, Collection<String> keys)
            throws IntegrityError {
        return updateVertex(id, type, data, keys, new LinkedList<String>());
    }

    public Vertex updateVertex(String id, String type,
            Map<String, Object> data, Collection<String> keys,
            Collection<String> uniqueKeys) throws IntegrityError {
        Index<Vertex> index = getIndex();
        Map<String, Object> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(id, type, keys);
        CloseableIterable<Vertex> get = getIndex().get(EntityType.ID_KEY, id);
        try {
            try {
                Vertex node = get.iterator().next();
                checkUniqueness(index, uniqueKeys, data, node);
                replaceProperties(index, node, indexData, indexKeys);
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.SUCCESS);
                return node;

            } catch (NoSuchElementException e) {
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.FAILURE);
                throw new RuntimeException(String.format(
                        "Item with id '%s' not found in index: %s", id,
                        INDEX_NAME));
            } catch (IntegrityError e) {
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.FAILURE);
                throw e;
            } catch (Exception e) {
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.FAILURE);
                throw new RuntimeException(e);
            }
        } finally {
            get.close();
        }
    }

    /**
     * List the given property for objects within a given index.
     * 
     * @param typeName
     * @param propertyName
     * @return
     */
    public List<Object> getAllPropertiesOfType(String typeName,
            String propertyName) {
        List<Object> out = new LinkedList<Object>();
        CloseableIterable<Vertex> query = getVertices(typeName);
        try {
            for (Vertex v : query) {
                out.add(v.getProperty(propertyName));
            }
        } finally {
            query.close();
        }
        return out;
    }

    private Index<Vertex> getIndex() {
        Index<Vertex> index = graph.getBaseGraph().getIndex(INDEX_NAME,
                Vertex.class);
        if (index == null) {
            index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
        }
        return index;
    }

    /**
     * Delete vertex with its edges Neo4j requires you delete all adjacent edges
     * first. Blueprints' removeVertex() method does that; the Neo4jServer
     * DELETE URI does not.
     * 
     * @param id
     *            The vertex identifier
     */
    public void deleteVertex(String id) {
        deleteVertex(getVertex(id));
    }

    /**
     * Delete vertex with its edges Neo4j requires you delete all adjacent edges
     * first. Blueprints' removeVertex() method does that; the Neo4jServer
     * DELETE URI does not.
     * 
     * @param vertex
     *            The vertex
     */
    public void deleteVertex(Vertex vertex) {
        Index<Vertex> index = getIndex();
        for (String key : vertex.getPropertyKeys()) {
            index.remove(key, vertex.getProperty(key), vertex);
        }
        graph.removeVertex(vertex);
        graph.getBaseGraph().stopTransaction(
                TransactionalGraph.Conclusion.SUCCESS);
    }

    /**
     * @param index
     * @param uniqueKeys
     * @param data
     * @throws IntegrityError
     */
    private void checkUniqueness(Index<Vertex> index,
            Collection<String> uniqueKeys, Map<String, Object> data,
            Vertex current) throws IntegrityError {
        if (uniqueKeys != null && uniqueKeys.size() != 0) {
            Map<String, String> clashes = new HashMap<String, String>();
            for (String ukey : uniqueKeys) {
                String uval = (String) data.get(ukey);
                if (uval != null && index.count(ukey, uval) > 0) {
                    CloseableIterable<Vertex> query = index.get(ukey, uval);
                    try {
                        Vertex other = query.iterator().next();
                        if (other != null && !other.equals(current)) {
                            clashes.put(ukey, uval);
                        }
                    } finally {
                        query.close();
                    }
                }
            }
            if (!clashes.isEmpty()) {
                throw new IntegrityError(index.getIndexName(), clashes);
            }
        }
    }

    /**
     * Replace properties to a property container like vertex and edge
     * 
     * @param index
     *            The index of the container
     * @param c
     *            The container Edge or Vertex of type <code>T</code>
     * @param data
     *            The properties
     */
    private <T extends Element> void replaceProperties(Index<T> index, T c,
            Map<String, Object> data, Collection<String> keys) {
        // remove 'old' properties
        for (String key : c.getPropertyKeys()) {
            if (keys == null || keys.contains(key)) {
                index.remove(key, c.getProperty(key), c);
            }
            c.removeProperty(key);
        }

        // add all 'new' properties to the relationship and index
        addProperties(index, c, data, keys);
    }

    /**
     * Add properties to a property container like vertex and edge
     * 
     * @param index
     *            The index of the container
     * @param c
     *            The container Edge or Vertex of type <code>T</code>
     * @param data
     *            The properties
     */
    private <T extends Element> void addProperties(Index<T> index, T c,
            Map<String, Object> data, Collection<String> keys) {
        // TODO data cannot be null

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            c.setProperty(entry.getKey(), entry.getValue());
            if (keys == null || keys.contains(entry.getKey()))
                index.put(entry.getKey(), String.valueOf(entry.getValue()), c);
        }
    }

    private void checkExists(Index<Vertex> index, String id)
            throws IntegrityError {
        long existing = index.count(EntityType.ID_KEY, id);
        if (existing != 0)
            throw new IntegrityError("Item exists with ID: " + id, "entities");
    }

    private Map<String, Object> getVertexData(String id, String type,
            Map<String, Object> data) {
        Map<String, Object> vdata = new HashMap<String, Object>(data);
        vdata.put(EntityType.ID_KEY, id);
        vdata.put(EntityType.TYPE_KEY, type);
        return vdata;
    }

    private Collection<String> getVertexKeys(String id, String type,
            Collection<String> keys) {
        List<String> vkeys = new LinkedList<String>(keys);
        vkeys.add(EntityType.ID_KEY);
        vkeys.add(EntityType.TYPE_KEY);
        return vkeys;
    }

    private String getLuceneQuery(String key, Object value, String type) {
        return String.format("%s:%s AND %s:%s", key, String.valueOf(value),
                EntityType.TYPE_KEY, type);
    }

    private org.neo4j.graphdb.index.Index<Node> getRawIndex() {
        IndexManager index = graph.getBaseGraph().getRawGraph().index();
        return index.forNodes(INDEX_NAME);
    }
}
