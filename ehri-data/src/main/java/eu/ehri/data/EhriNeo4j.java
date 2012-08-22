/**
 * TODO Add license text
 */
package eu.ehri.data;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;

import java.util.Iterator;
import java.util.Map;

/**
 * Main purpose is to be used by the ehri-plugin to provide a REST API to the neo4j service
 * Adds functionality that would otherwise require several neo4j calls 
 * and when possible also hides neo4j specifics and use more generic GrapgDb names.
 * neo4j Node => Vertex
 * neo4j Relationship => Edge 
 * 
 * @author paulboon
 *
 */
public class EhriNeo4j {
	/**
	 * 
	 */
	private EhriNeo4j() { }

	/**
	 * 
	 * @param graphDb
	 * @param index
	 * @param field
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static Iterator<Vertex> simpleQuery(
		GraphDatabaseService graphDb, String index, String field, String query) throws Exception {
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		Iterator<Vertex> iter = graph.getIndex(index, Vertex.class).get(field, query).iterator();
		return iter;
	}
	
	/*** Vertices ***/
	
	/**
	 * Create a vertex (also known as Node) and have it indexed
	 * 
	 * TODO maybe have it return a Vertex instead of a Node or rename it to createNodeIndexed?
	 * Also remove the Index from the name?
	 * 
	 * @param graphDb
	 * @param data Contains the properties of he node; key, value pairs
	 * @param indexName
	 * @return The node created
	 * @throws Exception
	 */
	public static Node createIndexedVertex(GraphDatabaseService graphDb, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		graph.setMaxBufferSize(0);
		graph.startTransaction();
		try {
			Index<Node> index = manager.forNodes(indexName);
			Node node = graphDb.createNode();
			
			addProperties(index, node, data);
			
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return node;
		} catch (Exception e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}
		
	/**
	 * Delete vertex with its edges
	 * Neo4j requires you delete all adjacent edges first. 
	 * Blueprints' removeVertex() method does that; the Neo4jServer DELETE URI does not.
	 * 
	 * @param graphDb
	 * @param id
	 */
	public static void deleteVertex(GraphDatabaseService graphDb, long id)
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);

		Vertex vertex = graph.getVertex(id);
		graph.removeVertex(vertex);
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
	public static Node updateIndexedVertex(GraphDatabaseService graphDb, long id, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		graph.setMaxBufferSize(0);
		graph.startTransaction();
		try {
			Index<Node> index = manager.forNodes(indexName);
			Node node = graphDb.getNodeById(id);
			
			replaceProperties(index, node, data);
		
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return node;
		} catch (Exception e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}
	
	/*** Edges ***/
	
	/**
	 * Create an indexed Edge
	 * 
	 * @param graphDb
	 * @param outV
	 * @param typeLabel
	 * @param inV
	 * @param data
	 * @param indexName
	 * @return
	 * @throws Exception
	 */
	public static Relationship createIndexedEdge(GraphDatabaseService graphDb, long outV, String typeLabel, long inV, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		// an enum needed here?
		DynamicRelationshipType relationshipType = DynamicRelationshipType.withName(typeLabel); 
		
		graph.setMaxBufferSize(0);
		graph.startTransaction();
		try {
			Node node = graphDb.getNodeById(outV);
			
			RelationshipIndex index = manager.forRelationships(indexName);
		
			Relationship relationship = node.createRelationshipTo(graphDb.getNodeById(inV), relationshipType);
			
			addProperties(index, relationship, data);
			
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return relationship;
		} catch (Exception e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}
	
	
	/**
	 * Delete Edge
	 * 
	 * @param graphDb
	 * @param id
	 */
	public static void deleteEdge(GraphDatabaseService graphDb, long id)
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		
		Edge edge = graph.getEdge(id);
		graph.removeEdge(edge);
	}
	
	/**
	 * Update Edge
	 * similar to update Vertex, because the Type cannot be changed
	 * 
	 * @param graphDb
	 * @param id
	 * @param data
	 * @param indexName
	 * @return
	 * @throws Exception
	 */
	public static Relationship updateIndexedEdge(GraphDatabaseService graphDb, long id, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		graph.setMaxBufferSize(0);
		graph.startTransaction();
		try {
			RelationshipIndex index = manager.forRelationships(indexName);
			Relationship relationship = graphDb.getRelationshipById(id);
			
			replaceProperties(index, relationship, data);
			
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return relationship;
		} catch (Exception e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}	
	
    /*** helpers ***/

	private static void replaceProperties(Index index, PropertyContainer c, Map<String, Object> data)
	{
		// remove container from index
		index.remove(c);
		
		// remove 'old' properties
		for (String key : c.getPropertyKeys())
			c.removeProperty(key);
		
		// add all 'new' properties to the relationship and index
		addProperties(index, c, data);
	}
	
	// add properties to a property container like vertex and edge
	private static void addProperties(Index index, PropertyContainer c, Map<String, Object> data)
	{
		// TODO data cannot be null
		
		for(Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() == null)
				continue;
			c.setProperty(entry.getKey(), entry.getValue());
			index.add(c, entry.getKey(), String.valueOf(entry.getValue()));
		}
	}
}