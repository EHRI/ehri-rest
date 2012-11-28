package eu.ehri.project.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexManager;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.persistance.EntityBundle;

public final class GraphManager {

    public static final String INDEX_NAME = "entities";

    private final FramedGraph<Neo4jGraph> graph;

    public GraphManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
    }

    public Vertex createVertex(String id, EntityBundle<?> bundle)
            throws IntegrityError {
        Index<Vertex> index = getIndex();
        try {
            checkUniqueness(index, bundle.getUniquePropertyKeys(),
                    bundle.getData(), null);
            List<String> keys = bundle.getPropertyKeys();

            long existing = index.count(EntityType.ID_KEY, id);
            if (existing != 0)
                throw new IntegrityError("Item exists with ID: " + id,
                        "entities");

            Vertex node = graph.addVertex(null);
            index.put(EntityType.ID_KEY, id, node);
            index.put(EntityType.TYPE_KEY, bundle.getEntityType(), node);
            for (Map.Entry<String, Object> entry : bundle.getData().entrySet()) {
                if (entry.getValue() == null)
                    continue;
                node.setProperty(entry.getKey(), entry.getValue());
                if (keys == null || keys.contains(entry.getKey())) {
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

    public Vertex updateVertex(EntityBundle<?> bundle) throws IntegrityError {
        Index<Vertex> index = getIndex();
        CloseableIterable<Vertex> get = getIndex().get(EntityType.ID_KEY,
                bundle.getId());
        try {
            try {
                Vertex node = get.iterator().next();
                checkUniqueness(index, bundle.getUniquePropertyKeys(),
                        bundle.getData(), node);
                replaceProperties(index, node, bundle.getData(),
                        bundle.getPropertyKeys());
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.SUCCESS);
                return node;

            } catch (NoSuchElementException e) {
                graph.getBaseGraph().stopTransaction(
                        TransactionalGraph.Conclusion.FAILURE);
                throw new RuntimeException(String.format(
                        "Item with id '%s' not found in index: %s",
                        bundle.getId(), INDEX_NAME));
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

    public CloseableIterable<Vertex> getVertices(String type) {
        return getIndex().get(EntityType.TYPE_KEY, type);
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

    public org.neo4j.graphdb.index.Index<Node> getRawIndex() {
        IndexManager index = graph.getBaseGraph().getRawGraph().index();
        return index.forNodes(INDEX_NAME);
    }

    public Index<Vertex> getIndex() {
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
     * @param graphDb
     *            The graph database
     * @param id
     *            The vertex identifier
     */
    public void deleteVertex(EntityBundle<?> bundle) {
        Vertex vertex = getVertex(bundle.getId());
        Index<Vertex> index = getIndex();
        for (String key : bundle.getPropertyKeys()) {
            index.remove(key, vertex.getProperty(key), vertex);
        }
        index.remove(EntityType.ID_KEY, vertex.getProperty(EntityType.ID_KEY), vertex);
        index.remove(EntityType.TYPE_KEY, vertex.getProperty(EntityType.TYPE_KEY), vertex);
        graph.removeVertex(vertex);
        graph.getBaseGraph().stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
    }
    
    /**
     * @param index
     * @param uniqueKeys
     * @param data
     * @throws IntegrityError
     */
    public void checkUniqueness(Index<Vertex> index, List<String> uniqueKeys,
            Map<String, Object> data, Vertex current) throws IntegrityError {
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
    public <T extends Element> void replaceProperties(Index<T> index, T c,
            Map<String, Object> data, List<String> keys) {
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
            Map<String, Object> data, List<String> keys) {
        // TODO data cannot be null

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            c.setProperty(entry.getKey(), entry.getValue());
            if (keys == null || keys.contains(entry.getKey()))
                index.put(entry.getKey(), String.valueOf(entry.getValue()), c);
        }
    }    
}
