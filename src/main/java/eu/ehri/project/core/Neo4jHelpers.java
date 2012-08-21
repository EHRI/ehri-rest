package eu.ehri.project.core;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Node;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jEdge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import org.neo4j.graphdb.DynamicRelationshipType;

import eu.ehri.project.exceptions.IndexNotFoundException;

import java.util.Iterator;
import java.util.Map;

public class Neo4jHelpers {
	public Neo4jHelpers(GraphDatabaseService g) { 
		graph = new Neo4jGraph(g);
	}
	
	protected Neo4jGraph graph;

	public Index<Vertex> getVertexIndex(String name) throws IndexNotFoundException {
		Index<Vertex> index = graph.getIndex(name, Vertex.class);
		if (index == null)
			throw new IndexNotFoundException(name);
		return index;
	}
	
	public Index<Edge> getEdgeIndex(String name) throws IndexNotFoundException {
		Index<Edge> index = graph.getIndex(name, Edge.class);
		if (index == null)
			throw new IndexNotFoundException(name);
		return index;
	}
	
	public Index<Vertex> createVertexIndex(String name) {
		return graph.createIndex(name,  Vertex.class);
	}
	
	public Index<Edge> createEdgeIndex(String name) {
		return graph.createIndex(name,  Edge.class);
	}
	
	public Index<Vertex> getOrCreateVertexIndex(String name) {
        try {
            return getVertexIndex(name);
        } catch (IndexNotFoundException e) {
    		return graph.createIndex(name,  Vertex.class);
        }
	}
	
	public Index<Edge> getOrCreateEdgeIndex(String name) {
        try {
            return getEdgeIndex(name);
        } catch (IndexNotFoundException e) {
            return graph.createIndex(name,  Edge.class);
        }
	}
	
	public Iterator<Vertex> simpleQuery(String index, String field, String query) 
			throws IndexNotFoundException {
		Iterator<Vertex> iter = getVertexIndex(index).get(field, query).iterator();
		return iter;
	}
	
	public Vertex createIndexedVertex(Map<String, Object> data, String indexName)
			throws IndexNotFoundException 
	{
		try {
			Index<Vertex> index = getVertexIndex(indexName);
			Vertex node = graph.addVertex(null);
			
			for(Map.Entry<String, Object> entry : data.entrySet()) {
				if (entry.getValue() == null)
					continue;
				node.setProperty(entry.getKey(), entry.getValue());
				index.put(entry.getKey(), String.valueOf(entry.getValue()), node);
			}
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return node;
		} catch (IndexNotFoundException e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}

    public Edge createIndexedEdge(Long src, Long dst, String label, Map<String, Object> data) 
            throws IndexNotFoundException {
        GraphDatabaseService g = graph.getRawGraph();
        return createIndexedEdge(g.getNodeById(src), g.getNodeById(dst), label, data);
    }

    public Edge createIndexedEdge(Neo4jVertex src, Neo4jVertex dst, String label, Map<String, Object> data) 
            throws IndexNotFoundException {
        return createIndexedEdge(src.getRawVertex(), dst.getRawVertex(), label, data);
    }

    public Edge createIndexedEdge(Node src, Node dst, String label, Map<String, Object> data) 
           throws IndexNotFoundException {
        DynamicRelationshipType relationshipType = DynamicRelationshipType.withName(label);

        try {
            Index<Edge> index = getEdgeIndex(label);
            Edge edge = new Neo4jEdge(src.createRelationshipTo(dst, relationshipType), graph);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                edge.setProperty(entry.getKey(), entry.getValue());
                index.put(entry.getKey(), String.valueOf(entry.getValue()), edge);
            }
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return edge;
		} catch (IndexNotFoundException e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
        }    
    }
}
