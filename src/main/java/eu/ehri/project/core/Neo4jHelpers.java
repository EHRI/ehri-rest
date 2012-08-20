package eu.ehri.project.core;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

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
	
	public Index<Vertex> createVertexIndex(String name) {
		return graph.createIndex(name,  Vertex.class);
	}
	
	public Iterator<Vertex> simpleQuery(String index, String field, String query) 
			throws IndexNotFoundException {
		Iterator<Vertex> iter = getVertexIndex(index).get(field, query).iterator();
		return iter;
	}
	
	public Vertex createIndexedVertex(Map<String, Object> data, String indexName)
			throws IndexNotFoundException 
	{
		//graph.getRawGraph().beginTx();		
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
}
