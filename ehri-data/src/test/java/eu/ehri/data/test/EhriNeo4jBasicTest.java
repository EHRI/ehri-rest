package eu.ehri.data.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.Vertex;

import eu.ehri.data.EhriNeo4j;

public class EhriNeo4jBasicTest {
	   protected GraphDatabaseService graphDb;

	   private static final String TEST_INDEX_NAME = "testIndex"; 
	   private static final String TEST_KEY = "testKey"; 
	   private static final String TEST_VALUE = "testValue"; 
	   
	    /**
	     * Create temporary database for each unit test.
	     */
	    @Before
	    public void prepareTestDatabase()
	    {
	        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
	    }

	    /**
	     * Shutdown the database.
	     */
	    @After
	    public void destroyTestDatabase()
	    {
	        graphDb.shutdown();
	    }
	    
	    @Test
	    public void testCreateIndexedVertex() throws Exception
	    {
	    	// TODO test if it handles null or empty strings etc.
	    	
			Node indexedVertex = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			long vertexId = indexedVertex.getId();
			assertEquals((String) indexedVertex.getProperty(TEST_KEY), TEST_VALUE);
			
			// do we have a node with that id and property in the neo4j database?
	        Node foundNode = graphDb.getNodeById( vertexId );
	        assertEquals(foundNode.getId(), vertexId );
	        assertEquals((String) foundNode.getProperty( TEST_KEY ), TEST_VALUE);
	    }

	    @Test (expected=org.neo4j.graphdb.NotFoundException.class)
	    public void testDeleteIndexedVertex() throws Exception
	    {
	    	// We need to create one first, sorry
			Node indexedVertex = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			long vertexId = indexedVertex.getId();
			
			EhriNeo4j.deleteVertex(graphDb, indexedVertex.getId());
			
			// we should not find the node in the neo4j database anymore
			graphDb.getNodeById( vertexId );
	    }
	    
	    // TODO test deleting non-existing vertex, and other bad input
	
	    @Test 
	    public void testUpdateIndexedVertex() throws Exception
	    {
	    	// We need to create one first, sorry
			Node indexedVertex = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			long vertexId = indexedVertex.getId();
		
			final String NEW_TEST_KEY = "newTestKey";
			final String NEW_TEST_VALUE = "newTestKey";
			
	    	Map<String, Object> data = new HashMap<String, Object>();
	    	data.put(TEST_KEY, NEW_TEST_VALUE); // change existing property
	    	data.put(NEW_TEST_KEY, NEW_TEST_VALUE); // add a new property

	    	Node updatedIndexedVertex = EhriNeo4j.updateIndexedVertex(graphDb, vertexId, data, TEST_INDEX_NAME);
	    	String changedValue = (String) updatedIndexedVertex.getProperty(TEST_KEY);

	    	assertEquals(changedValue, NEW_TEST_VALUE);
	    	String newValue = (String) updatedIndexedVertex.getProperty(NEW_TEST_KEY);
	    	assertEquals(newValue, NEW_TEST_VALUE);
	    }

	    @Test
	    public void testCreateIndexedEdge() throws Exception
	    {
	    	// TODO test if it handles null or empty strings etc.

	    	// create two Vertices first
			Node outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			Node inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			final String TEST_TYPE = "testType";
			
			// check that vertices have no relations before creation
			assertFalse(outV.hasRelationship());
			assertFalse(inV.hasRelationship());

			Relationship relationship = EhriNeo4j.createIndexedEdge(graphDb, outV.getId(), TEST_TYPE, inV.getId(), createTestData(TEST_KEY, TEST_VALUE), TEST_INDEX_NAME);
			long id = relationship.getId();
			
			// check that vertices have relations after creation
			assertTrue(outV.hasRelationship());
			assertTrue(inV.hasRelationship());

			// do we have a relationship with that id and property etc. in the neo4j database?
	        Relationship foundRelationship = graphDb.getRelationshipById(id);
	        assertEquals(foundRelationship.getId(), id );
	        assertEquals(foundRelationship.getStartNode().getId(), outV.getId());
	        assertEquals(foundRelationship.getEndNode().getId(), inV.getId());
	        assertEquals(foundRelationship.getType().toString(), TEST_TYPE); // ?
	        assertEquals((String) foundRelationship.getProperty( TEST_KEY ), TEST_VALUE);
	    }

	    @Test  (expected=org.neo4j.graphdb.NotFoundException.class)
	    public void testDeleteIndexedEdge() throws Exception
	    {
	    	// TODO test if it handles null or empty strings etc.

	    	// create two Vertices first
			Node outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			Node inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			final String TEST_TYPE = "testType";
			
			// create the Edge
			Relationship relationship = EhriNeo4j.createIndexedEdge(graphDb, outV.getId(), TEST_TYPE, inV.getId(), createTestData(TEST_KEY, TEST_VALUE), TEST_INDEX_NAME);
			long id = relationship.getId();
			
			// check that vertices have relations before delete
			assertTrue(outV.hasRelationship());
			assertTrue(inV.hasRelationship());

			// delete it
			EhriNeo4j.deleteEdge(graphDb, id);
			
			// check that vertices have no relations after delete
			assertFalse(outV.hasRelationship());
			assertFalse(inV.hasRelationship());
			
			// we should not have a relationship with that id and property etc. in the neo4j database
	        graphDb.getRelationshipById(id);
	    }
	    
	    @Test
	    public void testUpdateIndexedEdge() throws Exception
	    {
	    	// TODO test if it handles null or empty strings etc.

	    	final String NEW_TEST_KEY = "newTestKey";
			final String NEW_TEST_VALUE = "newTestKey";

	    	// create two Vertices first
			Node outV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			Node inV = createIndexedVertexWithProperty(TEST_KEY, TEST_VALUE);
			final String TEST_TYPE = "testType";
			
			// create the Edge
			Relationship relationship = EhriNeo4j.createIndexedEdge(graphDb, outV.getId(), TEST_TYPE, inV.getId(), createTestData(TEST_KEY, TEST_VALUE), TEST_INDEX_NAME);
			long id = relationship.getId();
			
	    	Map<String, Object> data = new HashMap<String, Object>();
	    	data.put(TEST_KEY, NEW_TEST_VALUE); // change existing property
	    	data.put(NEW_TEST_KEY, NEW_TEST_VALUE); // add a new property

	    	Relationship updatedIndexedEdge = EhriNeo4j.updateIndexedEdge(graphDb, id, data, TEST_INDEX_NAME);
	    	String changedValue = (String) updatedIndexedEdge.getProperty(TEST_KEY);

	    	assertEquals(changedValue, NEW_TEST_VALUE);
	    	String newValue = (String) updatedIndexedEdge.getProperty(NEW_TEST_KEY);
	    	assertEquals(newValue, NEW_TEST_VALUE);
	    }	    
	    
	    
	    @Test
	    public void testGetOrCreateVertexIndex() throws Exception
	    {
	    	final String NEW_INDEX_NAME = "newTestIndex";
	    	
	    	// make sure we don't have it
	    	Index<Vertex> vertexIndex = EhriNeo4j.getVertexIndex(graphDb, NEW_INDEX_NAME);
	    	assertEquals(vertexIndex, null);
	    	
	    	// create it
	    	vertexIndex = EhriNeo4j.getOrCreateVertexIndex(graphDb, NEW_INDEX_NAME, null);
	    	assertFalse(vertexIndex == null);
	    	
	    	// and check if we can find it via get
	    	vertexIndex = EhriNeo4j.getVertexIndex(graphDb, NEW_INDEX_NAME);
	    	assertFalse(vertexIndex == null);
	    }
	    
	    /*** helpers ***/
	    
	    private Node createIndexedVertexWithProperty(String key, String value) throws Exception
	    {	    	
	    	return EhriNeo4j.createIndexedVertex(graphDb, createTestData(key, value), TEST_INDEX_NAME);
	    }
	 	    
	    private Map<String, Object> createTestData(String key, String value)
	    {
	    	Map<String, Object> data = new HashMap<String, Object>();
	    	data.put(key, value);
	    	
	    	return data;
	    }
}
