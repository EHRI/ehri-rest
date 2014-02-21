package eu.ehri.project.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.WrappingCloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Frame;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes.
 * 
 * @author mike
 * 
 */
public final class BasicGraphManager<T extends IndexableGraph> implements GraphManager {

    private static final String INDEX_NAME = "entities";
    private static final String METADATA_PREFIX = "_";

    private final FramedGraph<T> graph;

    public FramedGraph<T> getGraph() {
        return graph;
    }

    public BasicGraphManager(FramedGraph<?> graph) {
        // Accept a warning here about the unsafe cast.
        this.graph = (FramedGraph<T>)graph;
    }

    // Access functions
    public String getId(Vertex vertex) {
        return vertex.getProperty(EntityType.ID_KEY);
    }

    public String getId(Frame frame) {
        return getId(frame.asVertex());
    }

    public String getType(Vertex vertex) {
        return vertex.getProperty(EntityType.TYPE_KEY);
    }

    public String getType(Frame frame) {
        return frame.getType();
    }

    public EntityClass getEntityClass(Vertex vertex) {
        Preconditions.checkNotNull(vertex);
        return EntityClass.withName(getType(vertex));
    }

    public EntityClass getEntityClass(Frame frame) {
        Preconditions.checkNotNull(frame);
        return EntityClass.withName(frame.getType());
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

    public <T> CloseableIterable<T> getFrames(EntityClass type, Class<T> cls) {
        CloseableIterable<Vertex> vertices = getVertices(type);
        try {
            return new WrappingCloseableIterable<T>(
                    graph.frameVertices(getVertices(type), cls));
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
        for (Vertex v : graph.getVertices(EntityType.ID_KEY, id)) {
            if (getEntityClass(v).equals(type))
                return v;
        }
        throw new ItemNotFound(id);
    }

    public CloseableIterable<Vertex> getVertices(EntityClass type) {
        return getIndex().get(EntityType.TYPE_KEY, type.getName());
    }

    public CloseableIterable<Vertex> getVertices(Iterable<String> ids) throws ItemNotFound {
        // Ugh, we don't want to remove duplicate results here
        // because that's not expected behaviour - if you give
        // an array with dups you expect the dups to come out...
        List<Vertex> verts = Lists.newLinkedList();
        for (String id : ids) {
            verts.add(getVertex(id));
        }
        return new WrappingCloseableIterable(verts);
    }

    public CloseableIterable<Vertex> getVertices(String key, Object value,
            final EntityClass type) {
        // NB: This is rather annoying.
        CloseableIterable<Vertex> query = getIndex().get(key, value);
        List<Vertex> elems = Lists.newArrayList();
        try {
            for (Vertex v : query) {
                if (getEntityClass(v).equals(type)) {
                    elems.add(v);
                }
            }
        } finally {
            query.close();
        }
        return new WrappingCloseableIterable<Vertex>(elems);
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
        return node;
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
                return node;

            } catch (NoSuchElementException e) {
                throw new RuntimeException(String.format(
                        "Item with id '%s' not found in index: %s", id,
                        INDEX_NAME));
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
     * @throws eu.ehri.project.exceptions.ItemNotFound
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
        vertex.remove();
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
            Object value = item.getProperty(key);
            if (!key.startsWith(METADATA_PREFIX)) {
                item.removeProperty(key);
                if (keys == null || keys.contains(key)) {
                    index.remove(key, value, item);
                }
            }
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

    private Index<Vertex> getIndex() {
        Index<Vertex> index = graph.getBaseGraph().getIndex(INDEX_NAME,
                Vertex.class);
        if (index == null) {
            index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
        }
        return index;
    }
}
