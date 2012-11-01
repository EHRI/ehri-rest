/**
 * TODO Add license text
 */
package eu.ehri.project.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.IntegrityError;

import org.apache.commons.lang3.StringUtils;

/**
 * Main purpose is to be used by the ehri-plugin to provide a REST API to the
 * neo4j service Adds functionality that would otherwise require several neo4j
 * calls and when possible also hides neo4j specifics and use more generic
 * GrapgDb names. neo4j Node => Vertex neo4j Relationship => Edge
 * 
 */
public class GraphHelpers {
    public GraphHelpers(GraphDatabaseService g) {
        graph = new Neo4jGraph(g);
    }

    protected Neo4jGraph graph;

    /**
     * List the given property for objects within a given index.
     * 
     * @param indexName
     * @param propertyName
     * @return
     */
    public List<Object> getAllPropertiesOfType(String indexName,
            String propertyName) {
        List<Object> out = new LinkedList<Object>();
        Index<Vertex> index = graph.getIndex(indexName, Vertex.class);
        if (index != null) {
            CloseableIterable<Vertex> query = index.query(propertyName, "*");
            try {
                for (Vertex v : query) {
                    out.add(v.getProperty(propertyName));
                }
            } finally {
                query.close();
            }
        }
        return out;
    }

    /**
     * Get an index of type <code>T</code>.
     * 
     * @param name
     * @param cls
     * @return
     * @throws IndexNotFoundException
     */
    public <T extends Element> Index<T> getIndex(String name, Class<T> cls)
            throws IndexNotFoundException {
        Index<T> index = graph.getIndex(name, cls);
        if (index == null)
            throw new IndexNotFoundException(name);
        return index;
    }

    /**
     * Create an index of type <code>T</code>.
     * 
     * @param name
     * @param cls
     * @return
     */
    public <T extends Element> Index<T> createIndex(String name, Class<T> cls,
            Parameter<?, ?>... parameters) {
        return graph.createIndex(name, cls, parameters);
    }

    /**
     * Get an index of type <code>T</code>, or create it if it does not exist.
     * 
     * @param name
     * @param cls
     * @return
     */
    public <T extends Element> Index<T> getOrCreateIndex(String name,
            Class<T> cls, Parameter<?, ?>... parameters) {
        try {
            return getIndex(name, cls);
        } catch (IndexNotFoundException e) {
            return graph.createIndex(name, cls, parameters);
        }
    }

    /**
     * Create an edge index.
     * 
     * @param name
     * @return
     */
    public Index<Edge> createEdgeIndex(String name) {
        return createIndex(name, Edge.class);
    }

    /**
     * Create a vertex index.
     * 
     * @param name
     * @return
     */
    public Index<Vertex> createVertexIndex(String name) {
        return createIndex(name, Vertex.class);
    }

    /**
     * Query a graph index for a node with the given field of value query.
     * 
     * @param index
     * @param field
     * @param query
     * @return
     * @throws IndexNotFoundException
     */
    public Iterator<Vertex> simpleQuery(String index, String field, String query)
            throws IndexNotFoundException {
        Iterator<Vertex> iter = getIndex(index, Vertex.class).get(field, query)
                .iterator();
        return iter;
    }

    /**
     * Create a vertex that is indexed using an index of the given name. All
     * properties given will be indexed.
     * 
     * @param data
     * @param indexName
     * @return
     * @throws IndexNotFoundException
     * @throws IntegrityError
     */
    public Vertex createIndexedVertex(Map<String, Object> data, String indexName)
            throws IndexNotFoundException, IntegrityError {
        Index<Vertex> index = getIndex(indexName, Vertex.class);
        return createIndexedVertex(data, index, null, null);
    }

    /**
     * Create a vertex that is indexed using an index of the given name.
     * 
     * @param data
     * @param indexName
     * @param keys
     * @return
     * @throws IndexNotFoundException
     * @throws IntegrityError
     */
    public Vertex createIndexedVertex(Map<String, Object> data,
            String indexName, List<String> keys) throws IndexNotFoundException,
            IntegrityError {
        Index<Vertex> index = getIndex(indexName, Vertex.class);
        return createIndexedVertex(data, index, keys, null);
    }

    /**
     * Create a vertex that is indexed using the given index. All properties
     * will be indexed.
     * 
     * @param data
     * @param index
     * @return
     * @throws IntegrityError
     */
    public Vertex createIndexedVertex(Map<String, Object> data,
            Index<Vertex> index) throws IntegrityError {
        return createIndexedVertex(data, index, null, null);
    }

    /**
     * Create a vertex that is indexed using the given index.
     * 
     * @param data
     * @param index
     * @param keys
     * @return
     * @throws IntegrityError
     */
    public Vertex createIndexedVertex(Map<String, Object> data,
            Index<Vertex> index, List<String> keys, List<String> uniqueKeys)
            throws IntegrityError {
        try {
            checkUniqueness(index, uniqueKeys, data, null);
            Vertex node = graph.addVertex(null);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                node.setProperty(entry.getKey(), entry.getValue());
                if (keys == null || keys.contains(entry.getKey())) {
                    index.put(entry.getKey(), String.valueOf(entry.getValue()),
                            node);
                }
            }
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return node;
        } catch (IntegrityError e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @param index
     * @param uniqueKeys
     * @param data
     * @throws IntegrityError
     */
    private void checkUniqueness(Index<Vertex> index, List<String> uniqueKeys,
            Map<String, Object> data, Vertex current) throws IntegrityError {
        if (uniqueKeys != null && uniqueKeys.size() != 0) {
            Map<String, String> clashes = new HashMap<String, String>();
            for (String ukey : uniqueKeys) {
                String uval = (String) data.get(ukey);
                if (index.count(ukey, uval) > 0) {
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
     * Create an edge that is indexed with an index of the same name as it's
     * label.
     * 
     * @param src
     * @param dst
     * @param label
     * @param data
     * @return
     * @throws IndexNotFoundException
     */
    public Edge createIndexedEdge(Object src, Object dst, String label,
            Map<String, Object> data, String indexName)
            throws IndexNotFoundException {
        return createIndexedEdge(graph.getVertex(src), graph.getVertex(dst),
                label, data, indexName, null);
    }

    /**
     * Create an edge that is indexed with an index of the same name as it's
     * label.
     * 
     * @param src
     * @param dst
     * @param label
     * @param data
     * @param indexName
     * @param keys
     * @return
     * @throws IndexNotFoundException
     */
    public Edge createIndexedEdge(Vertex src, Vertex dst, String label,
            Map<String, Object> data, String indexName, List<String> keys)
            throws IndexNotFoundException {
        Index<Edge> index = getIndex(indexName, Edge.class);
        return createIndexedEdge(src, dst, label, data, index, keys);
    }

    /**
     * Create an edge that is indexed with the given index. All properties will
     * be indexed.
     * 
     * @param src
     * @param dst
     * @param label
     * @param data
     * @param index
     * @return
     */
    public Edge createIndexedEdge(Vertex src, Vertex dst, String label,
            Map<String, Object> data, Index<Edge> index) {

        return createIndexedEdge(src, dst, label, data, index, null);
    }

    /**
     * Create an edge that is indexed with the given index.
     * 
     * @param src
     * @param dst
     * @param label
     * @param data
     * @param index
     * @param keys
     * @return
     */
    public Edge createIndexedEdge(Vertex src, Vertex dst, String label,
            Map<String, Object> data, Index<Edge> index, List<String> keys) {

        try {
            Edge edge = graph.addEdge(null, src, dst, label);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                edge.setProperty(entry.getKey(), entry.getValue());
                if (keys != null && keys.contains(entry.getKey())) {
                    index.put(entry.getKey(), String.valueOf(entry.getValue()),
                            edge);
                }
            }
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return edge;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    public Edge createOrUpdateIndexedEdge(Vertex src, Vertex dst, String label,
            Map<String, Object> data, Index<Edge> index, List<String> keys) {
        Edge edge = null;
        for (Edge e : src.getEdges(Direction.OUT, label)) {
            if (e.getVertex(Direction.IN).equals(dst))
                edge = e;
        }
        if (edge == null) {
            return createIndexedEdge(src, dst, label, data, index, keys);
        }

        return updateIndexedEdge(edge, data, index, keys);
    }

    /**
     * Update a vertex. All properties will be indexed.
     * 
     * @param graphDb
     * @param id
     * @param data
     * @param indexName
     * @return
     * @throws IntegrityError
     * @throws Exception
     */
    public Vertex updateIndexedVertex(Object id, Map<String, Object> data,
            String indexName) throws IndexNotFoundException, IntegrityError {
        return updateIndexedVertex(id, data, indexName, null);
    }

    /**
     * Update a vertex
     * 
     * @param graphDb
     * @param id
     * @param data
     * @param indexName
     * @param keys
     * @return
     * @throws IntegrityError
     * @throws Exception
     */
    public Vertex updateIndexedVertex(Object id, Map<String, Object> data,
            String indexName, List<String> keys) throws IndexNotFoundException,
            IntegrityError {
        Index<Vertex> index = getIndex(indexName, Vertex.class);
        return updateIndexedVertex(id, data, index, keys, null);
    }

    /**
     * Update a vertex using the given index.
     * 
     * @param id
     * @param data
     * @param index
     * @return
     * @throws IntegrityError
     */
    public Vertex updateIndexedVertex(Object id, Map<String, Object> data,
            Index<Vertex> index, List<String> keys, List<String> uniqueKeys)
            throws IntegrityError {
        try {
            Vertex node = graph.getVertex(id);
            checkUniqueness(index, uniqueKeys, data, node);
            replaceProperties(index, node, data, keys);
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return node;
        } catch (IntegrityError e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw e;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update an indexed edge. All properties will be indexed.
     * 
     * @param graphDb
     * @param id
     * @param data
     * @param indexName
     * @return
     * @throws Exception
     */
    public Edge updateIndexedEdge(Object id, Map<String, Object> data,
            String indexName) throws IndexNotFoundException {
        Index<Edge> index = getIndex(indexName, Edge.class);
        return updateIndexedEdge(id, data, index, null);
    }

    /**
     * Update Edge similar to update Vertex, because the Type cannot be changed
     * 
     * @param graphDb
     *            The graph database
     * @param id
     *            The edge identifier
     * @param data
     *            The properties
     * @param indexName
     *            The name of the index
     * @param keys
     *            Values to index
     * @return
     * @throws Exception
     */
    public Edge updateIndexedEdge(Object id, Map<String, Object> data,
            String indexName, List<String> keys) throws IndexNotFoundException {
        Index<Edge> index = getIndex(indexName, Edge.class);
        return updateIndexedEdge(id, data, index, keys);
    }

    /**
     * Update an indexed Edge.
     * 
     * @param id
     * @param data
     * @param index
     * @param keys
     * @return
     */
    public Edge updateIndexedEdge(Object id, Map<String, Object> data,
            Index<Edge> index, List<String> keys) {
        try {
            Edge relationship = graph.getEdge(id);

            replaceProperties(index, relationship, data, keys);

            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return relationship;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    /*** helpers ***/

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

    /**
     * Delete Edge
     * 
     * @param graphDb
     *            The graph database
     * @param id
     *            The edge identifier
     */
    public void deleteEdge(Index<Edge> index, Object id, List<String> keys) {
        Edge edge = graph.getEdge(id);
        for (String key : keys) {
            index.remove(key, edge.getProperty(key), edge);
        }
        graph.removeEdge(edge);
        graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
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
    public void deleteVertex(Index<Vertex> index, Object id, List<String> keys) {
        Vertex vertex = graph.getVertex(id);
        for (String key : keys) {
            index.remove(key, vertex.getProperty(key), vertex);
        }
        graph.removeVertex(vertex);
        graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
    }

    /**
     * Delete Edge
     * 
     * @param graphDb
     *            The graph database
     * @param id
     *            The edge identifier
     */
    public void deleteEdge(Object id) {
        Edge edge = graph.getEdge(id);
        graph.removeEdge(edge);
        graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
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
    public void deleteVertex(Object id) {
        Vertex vertex = graph.getVertex(id);
        graph.removeVertex(vertex);
        graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
    }

    // Helpers

    /*
     * Create a compount property name for all the unique keys
     */
    private String getUniqueKey(List<String> uniqueKeys) {
        Collections.sort(uniqueKeys);
        return StringUtils.join(uniqueKeys, "-");
    }

    /*
     * Create a compound property value for all the unique values.
     */
    private String getUniqueValue(List<String> uniqueKeys,
            Map<String, Object> data) {
        Collections.sort(uniqueKeys);
        List<String> values = new LinkedList<String>();
        for (String key : uniqueKeys) {
            Object val = data.get(key);
            if (val != null)
                values.add(val.toString());
        }
        return StringUtils.join(values, "-");
    }
}
