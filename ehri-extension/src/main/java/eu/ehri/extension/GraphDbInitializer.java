package eu.ehri.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Use this to initialize a 'clean' neo4j database so that it can be used with the internal API. 
 * For that we need at least all the indexes (Node&Relationships), 
 * and all permissions and one user of the admin group!
 * 
 * Note: maybe we could skip the things that are already there?
 * 
 * @author paulboon
 *
 */
public class GraphDbInitializer {

	// TODO use less string literals and more constants from the classes
	
	/**
	 * Provide the path to the neo4j database: <neo4j-home>/data/graph.db
	 * Note that you need to stop the server that uses the db!
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		//String dbDir = (String) args[0];
		// TODO don't hardcode
		String dbDir = "/Users/paulboon/Documents/Development/neo4j-community-1.9-SNAPSHOT-EHRI/data/graph.db";
				
		System.out.println("Initializing Db for use with the ehri extension...");
	
		FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                new Neo4jGraph(dbDir));
        try {                    
            init(graph);
        } catch(Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            graph.shutdown();
    		System.out.println("Done initializing db.");
       }        
 	}
	
	public static void init(FramedGraph<Neo4jGraph> graph) throws Exception {
		GraphHelpers helpers = new GraphHelpers(graph.getBaseGraph()
				.getRawGraph());
		
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			createAdmin(helpers);
			createPermissions(helpers);
			createIndexes(helpers);
			
			tx.success();
		} catch (Exception e) {
			tx.failure();
			throw e;
		} finally {
			tx.finish();
		}
	}

	/**
	 * Note: maybe we could use reflection to get all indexes needed from annotations on the model objects?
	 * 
	 * @param helpers
	 * @throws Exception
	 */
	private static void createIndexes(GraphHelpers helpers)  throws Exception {
		
		// cvoc
		helpers.createVertexIndex("cvocConcept");
		helpers.createVertexIndex("cvocText");
		helpers.createEdgeIndex("narrower");
		helpers.createEdgeIndex("related");
		helpers.createEdgeIndex("prefLabel");
		helpers.createEdgeIndex("altLabel");
		helpers.createEdgeIndex("scopeNote");
		helpers.createEdgeIndex("definition");
		
		// TODO create all other indexes
	}

	@SuppressWarnings("serial")
	private static void createAdmin(GraphHelpers helpers)  throws Exception {
		helpers.createVertexIndex("group");
	    Map<String, Object> data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "admin");
	    	put(EntityType.KEY, "group");
	    	put("name", "Admin");
	    }};
		Vertex groupVertex = helpers.createIndexedVertex(data, "group");
		
		helpers.createVertexIndex("userProfile");
	    data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "admin");
	    	put(EntityType.KEY, "userProfile");
	    	put("name", "Admin");
	    }};
		Vertex userVertex = helpers.createIndexedVertex(data, "userProfile");
	
		helpers.createEdgeIndex("belongsTo");
		helpers.createIndexedEdge(userVertex, groupVertex, 
				"belongsTo", 
				Collections.<String, Object> emptyMap(), 
				"belongsTo", 
				Collections.<String> emptyList());
	}
	
	@SuppressWarnings("serial")
	private static void createPermissions(GraphHelpers helpers)  throws Exception {
		helpers.createVertexIndex("permission");
	
		// TODO get permission mask values from the library
		
		Map<String, Object> data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "create");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 1);
	    	put("name", "Create");
	    }};
		helpers.createIndexedVertex(data, "permission");
		
		data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "update");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 2);
	    	put("name", "Edit");
	    }};
		helpers.createIndexedVertex(data, "permission");
		
		data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "delete");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 4);
	    	put("name", "Delete");
	    }};
		helpers.createIndexedVertex(data, "permission");

		data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "grant");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 8);
	    	put("name", "Grant");
	    }};
		helpers.createIndexedVertex(data, "permission");
		
		data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "annotate");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 16);
	    	put("name", "Annotate");
	    }};
		helpers.createIndexedVertex(data, "permission");

		data = new HashMap<String, Object>() {{
	    	put(AccessibleEntity.IDENTIFIER_KEY, "owner");
	    	put(EntityType.KEY, "permission");
	    	put("mask", 23); // 32 = 16+4+2+1, so no 8=grant
	    	put("name", "Owner");
	    }};
		helpers.createIndexedVertex(data, "permission");

	}
}
