package eu.ehri.project.core;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jEdge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

import eu.ehri.project.exceptions.IndexNotFoundException;

public class GraphHelpers {
    public GraphHelpers(GraphDatabaseService g) {
        graph = new Neo4jGraph(g);
    }

    protected Neo4jGraph graph;

    public <T extends Element> Index<T> getIndex(String name, Class<T> cls)
            throws IndexNotFoundException {
        Index<T> index = graph.getIndex(name, cls);
        if (index == null)
            throw new IndexNotFoundException(name);
        return index;
    }

    public <T extends Element> Index<T> createIndex(String name, Class<T> cls) {
        return graph.createIndex(name, cls);
    }

    public <T extends Element> Index<T> getOrCreateIndex(String name,
            Class<T> cls) {
        try {
            return getIndex(name, cls);
        } catch (IndexNotFoundException e) {
            return graph.createIndex(name, cls);
        }
    }

    public Index<Edge> createdEdgeIndex(String name) {
        return createIndex(name, Edge.class);
    }

    public Index<Vertex> createVertexIndex(String name) {
        return createIndex(name, Vertex.class);
    }

    public Iterator<Vertex> simpleQuery(String index, String field, String query)
            throws IndexNotFoundException {
        Iterator<Vertex> iter = getIndex(index, Vertex.class).get(field, query)
                .iterator();
        return iter;
    }

    public Vertex createIndexedVertex(Map<String, Object> data, String indexName)
            throws IndexNotFoundException {
        Index<Vertex> index = getIndex(indexName, Vertex.class);
        return createIndexedVertex(data, index);
    }

    public Vertex createIndexedVertex(Map<String, Object> data,
            Index<Vertex> index) {
        try {

            Vertex node = graph.addVertex(null);

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                node.setProperty(entry.getKey(), entry.getValue());
                index.put(entry.getKey(), String.valueOf(entry.getValue()),
                        node);
            }
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return node;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Edge createIndexedEdge(Long src, Long dst, String label,
            Map<String, Object> data, Index<Edge> index) {
        GraphDatabaseService g = graph.getRawGraph();
        return createIndexedEdge(g.getNodeById(src), g.getNodeById(dst), label,
                data, index);
    }

    public Edge createIndexedEdge(Long src, Long dst, String label,
            Map<String, Object> data) throws IndexNotFoundException {
        GraphDatabaseService g = graph.getRawGraph();
        return createIndexedEdge(g.getNodeById(src), g.getNodeById(dst), label,
                data);
    }

    public Edge createIndexedEdge(Object src, Object dst, String label,
            Map<String, Object> data) throws IndexNotFoundException {
        return createIndexedEdge(graph.getVertex(src), graph.getVertex(dst), label,
                data);
    }

    public Edge createIndexedEdge(Neo4jVertex src, Neo4jVertex dst,
            String label, Map<String, Object> data)
            throws IndexNotFoundException {
        return createIndexedEdge(src.getRawVertex(), dst.getRawVertex(), label,
                data);
    }

    public Edge createIndexedEdge(Node src, Node dst, String label,
            Map<String, Object> data) throws IndexNotFoundException {
        Index<Edge> index = getIndex(label, Edge.class);
        return createIndexedEdge(src, dst, label, data, index);
    }

    public Edge createIndexedEdge(Node src, Node dst, String label,
            Map<String, Object> data, Index<Edge> index) {

        DynamicRelationshipType relationshipType = DynamicRelationshipType
                .withName(label);

        try {
            Edge edge = new Neo4jEdge(src.createRelationshipTo(dst,
                    relationshipType), graph);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                edge.setProperty(entry.getKey(), entry.getValue());
                index.put(entry.getKey(), String.valueOf(entry.getValue()),
                        edge);
            }
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return edge;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a vertex
     * 
     * @param graphDb
     * @param id
     * @param data
     * @param indexName
     * @return
     * @throws Exception
     */
    public Vertex updateIndexedVertex(long id, Map<String, Object> data,
            String indexName) throws IndexNotFoundException {
        Index<Vertex> index = getIndex(indexName, Vertex.class);
        return updateIndexedVertex(id, data, index);
    }

    public Vertex updateIndexedVertex(long id, Map<String, Object> data,
            Index<Vertex> index) {
        try {
            Vertex node = graph.getVertex(id);
            replaceProperties(index, node, data);

            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            return node;
        } catch (Exception e) {
            graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    private <T extends Element> void replaceProperties(Index<T> index, T c,
            Map<String, Object> data) {
        // remove 'old' properties
        for (String key : c.getPropertyKeys()) {
            index.remove(key, c.getProperty(key), c);
            c.removeProperty(key);
        }

        // add all 'new' properties to the relationship and index
        addProperties(index, c, data);
    }

    // add properties to a property container like vertex and edge
    private <T extends Element> void addProperties(Index<T> index, T c,
            Map<String, Object> data) {
        // TODO data cannot be null

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null)
                continue;
            c.setProperty(entry.getKey(), entry.getValue());
            index.put(entry.getKey(), String.valueOf(entry.getValue()), c);
        }
    }
}
