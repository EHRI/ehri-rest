package eu.ehri.project.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertexIterable;
import com.tinkerpop.blueprints.util.WrappingCloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Frame;
import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Implementation of GraphManager that uses a single index to manage all nodes,
 * with Neo4j Lucene query optimisations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class Neo4jGraphManager<T extends Neo4jGraph> implements GraphManager {

    private static final String INDEX_NAME = "entities";
    private static final String METADATA_PREFIX = "_";

    private final FramedGraph<T> graph;

    public FramedGraph<T> getGraph() {
        return graph;
    }

    public Neo4jGraphManager(FramedGraph<T> graph) {
        this.graph = graph;
    }

    public <T extends Frame> T cast(Frame frame, Class<T> cls) {
        return graph.frame(frame.asVertex(), cls);
    }

    public String getId(Vertex vertex) {
        Preconditions.checkNotNull(vertex);
        return vertex.getProperty(EntityType.ID_KEY);
    }

    public String getId(Frame frame) {
        Preconditions.checkNotNull(frame);
        return getId(frame.asVertex());
    }

    public String getType(Vertex vertex) {
        Preconditions.checkNotNull(vertex);
        return vertex.getProperty(EntityType.TYPE_KEY);
    }

    public String getType(Frame frame) {
        Preconditions.checkNotNull(frame);
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
            return new WrappingCloseableIterable<T>(graph.frameVertices(getVertices(type), cls));
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
        Preconditions.checkNotNull(type, "EntityClass is null in vertex/type count!");
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
        return new WrappingCloseableIterable<Vertex>(verts);
    }

    public CloseableIterable<Vertex> getVertices(String key, Object value,
            EntityClass type) {
        String queryStr = getLuceneQuery(key, value, type.getName());
        IndexHits<Node> rawQuery = getRawIndex().query(queryStr);
        return (CloseableIterable<Vertex>)new Neo4jVertexIterable(rawQuery,
                graph.getBaseGraph(), false);
    }

    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data) throws IntegrityError {
        return createVertex(id, type, data, data.keySet());
    }

    public Vertex createVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws IntegrityError {
        Preconditions
                .checkNotNull(id, "null vertex ID given for item creation");
        Index<Vertex> index = getIndex();
        Map<String, ?> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        checkExists(index, id);
        Vertex node = graph.addVertex(null);
        for (Map.Entry<String, ?> entry : indexData.entrySet()) {
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
            Map<String, ?> data) throws ItemNotFound {
        return updateVertex(id, type, data, data.keySet());
    }

    public Vertex updateVertex(String id, EntityClass type,
            Map<String, ?> data, Iterable<String> keys) throws ItemNotFound {
        Preconditions.checkNotNull(id, "null vertex ID given for item update");
        Index<Vertex> index = getIndex();
        Map<String, ?> indexData = getVertexData(id, type, data);
        Collection<String> indexKeys = getVertexKeys(keys);
        CloseableIterable<Vertex> get = getIndex().get(EntityType.ID_KEY, id);
        try {
            try {
                Vertex node = get.iterator().next();
                replaceProperties(index, node, indexData, indexKeys);
                return node;

            } catch (NoSuchElementException e) {
                throw new ItemNotFound(id);
            }
        } finally {
            get.close();
        }
    }

    @Override
    public void setProperty(Vertex vertex, String key, Object value) {
        Preconditions.checkNotNull(vertex);
        Preconditions.checkNotNull(key);
        Preconditions.checkArgument(
                !(key.trim().isEmpty() ||
                        key.equals(EntityType.ID_KEY) || key.equals(EntityType.TYPE_KEY)),
                "Invalid property key: %s", key);
        Index<Vertex> index = getIndex();
        Object current = vertex.getProperty(key);
        if (current != null) {
            index.remove(key, current, vertex);
        }
        if (value == null) {
            vertex.removeProperty(key);
        } else {
            vertex.setProperty(key, value);
            index.put(key, value, vertex);
        }
    }

    public void deleteVertex(String id) throws ItemNotFound {
        deleteVertex(getVertex(id));
    }

    public void deleteVertex(Vertex vertex) {
        Index<Vertex> index = getIndex();
        for (String key : vertex.getPropertyKeys()) {
            index.remove(key, vertex.getProperty(key), vertex);
        }
        vertex.remove();
    }

    private <T extends Element> void replaceProperties(Index<T> index, T item,
            Map<String, ?> data, Collection<String> keys) {
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

    private <T extends Element> void addProperties(Index<T> index, T item,
            Map<String, ?> data, Collection<String> keys) {
        Preconditions.checkNotNull(data, "Data map cannot be null");
        for (Map.Entry<String, ?> entry : data.entrySet()) {
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

    private Map<String, ?> getVertexData(String id, EntityClass type,
            Map<String, ?> data) {
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
