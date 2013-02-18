package eu.ehri.project.core.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.*;
import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertexIterable;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
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

    public FramedGraph<? extends TransactionalGraph> getGraph() {
        return graph;
    }

    public SingleIndexGraphManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
    }

    // Access functions
    public String getId(Vertex vertex) {
        return (String) vertex.getProperty(EntityType.ID_KEY);
    }

    public String getId(VertexFrame vertex) {
        return getId(vertex.asVertex());
    }

    public EntityClass getType(Vertex vertex) {
        return EntityClass.withName((String) vertex
                .getProperty(EntityType.TYPE_KEY));
    }

    public EntityClass getType(VertexFrame vertex) {
        return getType(vertex.asVertex());
    }

    public boolean exists(String id) {
        Preconditions.checkNotNull(id,
                "attempt determine existence of a vertex with a null id");
        return getIndex().count(EntityType.ID_KEY, id) > 0L;
    }

    public boolean propertyValueExists(String key, Object value) {
        Preconditions.checkNotNull(key,
                "attempt determine existence of a property value with a null name");
        Preconditions.checkNotNull(value,
                "attempt determine existence of a property given a null value");
        return getIndex().count(key, String.valueOf(value)) > 0L;
    }

    public <T> T getFrame(String id, Class<T> cls) throws ItemNotFound {
        return graph.frame(getVertex(id), cls);
    }

    public <T> T getFrame(String id, EntityClass type, Class<T> cls)
            throws ItemNotFound {
        return graph.frame(getVertex(id, type), cls);
    }

    public <T> Iterable<T> getFrames(EntityClass type, Class<T> cls) {
        CloseableIterable<Vertex> vertices = getVertices(type);
        try {
            return graph.frameVertices(getVertices(type), cls);
        } finally {
            vertices.close();
        }
    }

    public Vertex getVertex(String id) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        CloseableIterable<Vertex> query = getIndex().get(EntityType.ID_KEY, id);
        try {
            return query.iterator().next();
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        } finally {
            query.close();
        }
    }

    public Vertex getVertex(String id, EntityClass type) throws ItemNotFound {
        Preconditions
                .checkNotNull(id, "attempt to fetch vertex with a null id");
        String queryStr = getLuceneQuery(EntityType.ID_KEY, id, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        // NB: Not using rawQuery.getSingle here so we throw NoSuchElement
        // other than return null.
        try {
            return new Neo4jVertex(rawQuery.iterator().next(),
                    graph.getBaseGraph());
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(id);
        } finally {
            rawQuery.close();
        }
    }

    public CloseableIterable<Vertex> getVertices(EntityClass type) {
        return getIndex().get(EntityType.TYPE_KEY, type.getName());
    }

    @SuppressWarnings("unchecked")
    public CloseableIterable<Neo4jVertex> getVertices(String key, Object value,
            EntityClass type) {
        String queryStr = getLuceneQuery(key, value, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return new Neo4jVertexIterable<Vertex>(rawQuery, graph.getBaseGraph(),
                false);
    }

    public Vertex createVertex(String id, EntityClass type,
            Map<String, Object> data) throws IntegrityError {
        return createVertex(id, type, data, data.keySet());
    }

    public Vertex createVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys) throws IntegrityError {
        Preconditions
                .checkNotNull(id, "null vertex ID given for item creation");
        Index<Vertex> index = getIndex();
        Map<String, Object> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        try {
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

    public Vertex updateVertex(String id, EntityClass type,
            Map<String, Object> data) throws ItemNotFound {
        return updateVertex(id, type, data, data.keySet());
    }

    public Vertex updateVertex(String id, EntityClass type,
            Map<String, Object> data, Iterable<String> keys) throws ItemNotFound {
        Preconditions.checkNotNull(id, "null vertex ID given for item update");
        Index<Vertex> index = getIndex();
        Map<String, Object> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        CloseableIterable<Vertex> get = getIndex().get(EntityType.ID_KEY, id);
        try {
            try {
                Vertex node = get.iterator().next();
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
     * Delete vertex with its edges Neo4j requires you delete all adjacent edges
     * first. Blueprints' removeVertex() method does that; the Neo4jServer
     * DELETE URI does not.
     * 
     * @param id
     *            The vertex identifier
     * @throws ItemNotFound
     */
    public void deleteVertex(String id) throws ItemNotFound {
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
     * Replace properties to a property container like vertex and edge
     * 
     * @param index
     *            The index of the container
     * @param item
     *            The container Edge or Vertex of type <code>T</code>
     * @param data
     *            The properties
     */
    private <T extends Element> void replaceProperties(Index<T> index, T item,
            Map<String, Object> data, Collection<String> keys) {
        // remove 'old' properties
        for (String key : item.getPropertyKeys()) {
            if (keys == null || keys.contains(key)) {
                index.remove(key, item.getProperty(key), item);
            }
            item.removeProperty(key);
        }

        // add all 'new' properties to the relationship and index
        addProperties(index, item, data, keys);
    }

    /**
     * Add properties to a property container like vertex and edge
     * 
     * @param index
     *            The index of the container
     * @param item
     *            The container Edge or Vertex of type <code>T</code>
     * @param data
     *            The properties
     */
    private <T extends Element> void addProperties(Index<T> index, T item,
            Map<String, Object> data, Collection<String> keys) {
        Preconditions.checkNotNull(data, "Data map cannot be null");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            item.setProperty(entry.getKey(), entry.getValue());
            if (keys == null || keys.contains(entry.getKey()))
                index.put(entry.getKey(), String.valueOf(entry.getValue()),
                        item);
        }
    }

    private void checkExists(Index<Vertex> index, String id)
            throws IntegrityError {
        if (index.count(EntityType.ID_KEY, id) != 0) {
            throw new IntegrityError(id);
        }
    }

    private Map<String, Object> getVertexData(String id, EntityClass type,
            Map<String, Object> data) {
        Map<String, Object> vdata = Maps.newHashMap(data);
        vdata.put(EntityType.ID_KEY, id);
        vdata.put(EntityType.TYPE_KEY, type.getName());
        return vdata;
    }

    private Collection<String> getVertexKeys(Iterable<String> keys) {
        List<String> vkeys = Lists.newLinkedList(keys);
        vkeys.add(EntityType.ID_KEY);
        vkeys.add(EntityType.TYPE_KEY);
        return vkeys;
    }

    private org.neo4j.graphdb.index.Index<Node> getRawIndex() {
        IndexManager index = graph.getBaseGraph().getRawGraph().index();
        return index.forNodes(INDEX_NAME);
    }

    private Index<Vertex> getIndex() {
        Index<Vertex> index = graph.getBaseGraph().getIndex(INDEX_NAME,
                Vertex.class);
        if (index == null) {
            index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
        }
        return index;
    }

    private String getLuceneQuery(String key, Object value, String type) {
        return String.format("%s:\"%s\" AND %s:\"%s\"",
                QueryParser.escape(key),
                QueryParser.escape(String.valueOf(value)),
                QueryParser.escape(EntityType.TYPE_KEY),
                QueryParser.escape(type));
    }
}
