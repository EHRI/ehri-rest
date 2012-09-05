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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Main purpose is to be used by the ehri-plugin to provide a REST API to the neo4j service
 * Adds functionality that would otherwise require several neo4j calls 
 * and when possible also hides neo4j specifics and use more generic GraphDb names.
 * neo4j Node => Vertex
 * neo4j Relationship => Edge 
 * 
 */
public class EhriNeo4j {
	/**
	 * 
	 */
	private EhriNeo4j() { }

	/**
	 * 
	 * @param graphDb	The graph database
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
	 * @param graphDb		The graph database
	 * @param data 			The properties
	 * @param indexName		The name of the index
	 * @return 				The node created
	 * @throws Exception
	 */
	public static Node createIndexedVertex(GraphDatabaseService graphDb, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		//graph.setMaxBufferSize(0);
		//graph.startTransaction();
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
	 * @param graphDb	The graph database
	 * @param id		The vertex identifier
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
	 * @param graphDb	The graph database
	 * @param id		The vertex identifier
	 * @param data		The properties
	 * @param indexName	The name of the index
	 * @return
	 * @throws Exception
	 */
	public static Node updateIndexedVertex(GraphDatabaseService graphDb, long id, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		//graph.setMaxBufferSize(0);
		//graph.startTransaction();
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
	 * @param graphDb	The graph database
	 * @param outV		The outgoing vertex
	 * @param typeLabel	The edge type
	 * @param inV		The ingoing Vertex
	 * @param data		The properties
	 * @param indexName	The name of the index
	 * @return
	 * @throws Exception
	 */
	public static Relationship createIndexedEdge(GraphDatabaseService graphDb, long outV, String typeLabel, long inV, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		// an enum needed here?
		DynamicRelationshipType relationshipType = DynamicRelationshipType.withName(typeLabel); 
		
		//graph.setMaxBufferSize(0);
		//graph.startTransaction();
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
	 * @param graphDb	The graph database
	 * @param id		The edge identifier
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
	 * @param graphDb	The graph database
	 * @param id		The edge identifier
	 * @param data		The properties
	 * @param indexName	The name of the index
	 * @return
	 * @throws Exception
	 */
	public static Relationship updateIndexedEdge(GraphDatabaseService graphDb, long id, Map<String, Object> data, String indexName) throws Exception 
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		IndexManager manager = graphDb.index();
		
		//graph.setMaxBufferSize(0);
		//graph.startTransaction();
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
	
	// create_indexed_vertex_with_subordinates(data, index_name, subs)
	// update_indexed_vertex_with_subordinates(_id, data, index_name, subs) 
	  
	/*** index ***/
	
	// NOTE on Indexes
	//
	// For non-default configs... using params, otherwise you don't need it
	// so if we want lucene fulltext  we need to, because that is not the default!
	//
	// Also note that now we have no deleteIndex.

	/**
	 * 
	 * @param graphDb 		The graph database
	 * @param indexName 	The name of the index
	 * @return 				The index
	 */
	public static com.tinkerpop.blueprints.Index<Vertex> getVertexIndex(GraphDatabaseService graphDb, String indexName)
	{		
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		com.tinkerpop.blueprints.Index<Vertex> index = graph.getIndex(indexName, Vertex.class);
		
		return index;
	}	
	
	/**
	 * 
	 * @param graphDb 		The graph database
	 * @param indexName 	The name of the index
	 * @param parameters 	Index configuration parameters
	 * @return 				The index
	 * @throws Exception
	 */
	public static com.tinkerpop.blueprints.Index<Vertex> getOrCreateVertexIndex(GraphDatabaseService graphDb, String indexName, Map<String, Object> parameters) throws Exception
	{
		return getOrCreateIndex(graphDb, indexName, parameters, Vertex.class);
	}
	
	/**
	 * 
	 * @param graphDb 		The graph database
	 * @param indexName 	The name of the index
	 * @return 				The index
	 */
	public static com.tinkerpop.blueprints.Index<Edge> getEdgeIndex(GraphDatabaseService graphDb, String indexName)
	{		
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		com.tinkerpop.blueprints.Index<Edge> index = graph.getIndex(indexName, Edge.class);
		
		return index;
	}	
	
	/**
	 * 
	 * @param graphDb 		The graph database
	 * @param indexName 	The name of the index
	 * @param parameters 	Index configuration parameters
	 * @return The index
	 * @throws Exception
	 */
	public static com.tinkerpop.blueprints.Index<Edge> getOrCreateEdgeIndex(GraphDatabaseService graphDb, String indexName, Map<String, Object> parameters) throws Exception
	{
		return getOrCreateIndex(graphDb, indexName, parameters, Edge.class);
	}
	
	/**
	 * Generic version for Vertex and Edge classes
	 * 
	 * @param graphDb 		The graph database
	 * @param indexName 	The name of the index
	 * @param parameters 	Index configuration parameters
	 * @param indexClass 	The class of the index elements; Vertex or Edge
	 * @return 				The index
	 * @throws Exception
	 */
	private static <T extends Element> com.tinkerpop.blueprints.Index<T> getOrCreateIndex(GraphDatabaseService graphDb, String indexName, Map<String, Object> parameters, Class<T> indexClass) throws Exception
	{
		Neo4jGraph graph = new Neo4jGraph(graphDb);
		
		//graph.setMaxBufferSize(0);
		//graph.startTransaction();
		
		try {
			com.tinkerpop.blueprints.Index<T> index = 
					graph.getIndex(indexName, indexClass);

			if (index == null) {
				// create it
				if (parameters == null || parameters.isEmpty()) {
					// no parameters
					index = graph.createIndex(indexName, indexClass);
				} else {
					// construct List from parameter Map
					// and then have the list in place of the varargs
					ArrayList<Parameter> parametersList = new ArrayList<Parameter>();
					for(Map.Entry<String, Object> entry : parameters.entrySet()) {
						if (entry.getValue() == null)
							continue;
						parametersList.add(new Parameter<String, Object>(entry.getKey(), entry.getValue()));
					}
					
					index = graph.createIndex(indexName, indexClass, 
						parametersList.toArray(new Parameter[parametersList.size()]));
				}
			}
			graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
			return index;
		} catch (Exception e) {
			graph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
			throw e;
		}
	}
	
    /*** helpers ***/

	/**
	 * Replace properties to a property container like vertex and edge
	 * 
	 * @param index		The index of the container
	 * @param c			The container Edge or Vertex
	 * @param data		The properties
	 */
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
	
	/**
	 * Add properties to a property container like vertex and edge
	 * 
	 * @param index	The index of the container
	 * @param c		The container Edge or Vertex
	 * @param data	The properties
	 */
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