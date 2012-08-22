package eu.ehri.data.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;

//import static org.hamcrest.Matchers.*; 
import static org.junit.Assert.*;

/**
 * Uses the embedded neo4j database
 * initial code from the neo4j tutorials 
 *
 */
public class Neo4jBasicTest {
	   protected GraphDatabaseService graphDb;

	    /**
	     * Create temporary database for each unit test.
	     */
	    // START SNIPPET: beforeTest
	    @Before
	    public void prepareTestDatabase()
	    {
	        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
	    }
	    // END SNIPPET: beforeTest

	    /**
	     * Shutdown the database.
	     */
	    // START SNIPPET: afterTest
	    @After
	    public void destroyTestDatabase()
	    {
	        graphDb.shutdown();
	    }
	    // END SNIPPET: afterTest

	    @Test
	    public void startWithConfiguration()
	    {
	        // START SNIPPET: startDbWithConfig
	        Map<String, String> config = new HashMap<String, String>();
	        config.put( "neostore.nodestore.db.mapped_memory", "10M" );
	        config.put( "string_block_size", "60" );
	        config.put( "array_block_size", "300" );
	        GraphDatabaseService db = new ImpermanentGraphDatabase( config );
	        // END SNIPPET: startDbWithConfig
	        db.shutdown();
	    }

	    @Test
	    public void shouldCreateNode()
	    {
	        // START SNIPPET: unitTest
	        Transaction tx = graphDb.beginTx();

	        Node n = null;
	        try
	        {
	            n = graphDb.createNode();
	            n.setProperty( "name", "Nancy" );
	            tx.success();
	        }
	        catch ( Exception e )
	        {
	            tx.failure();
	        }
	        finally
	        {
	            tx.finish();
	        }

	        // The node should have an id greater than 0, which is the id of the
	        // reference node.
	        //assertThat( n.getId(), is( greaterThan( 0l ) ) );
	        assertNotNull(n.getId());
	        assertTrue(n.getId() > 0);
	        
	        // Retrieve a node by using the id of the created node. The id's and
	        // property should match.
	        Node foundNode = graphDb.getNodeById( n.getId() );
	        //assertThat( foundNode.getId(), is( n.getId() ) );
	        //assertThat( (String) foundNode.getProperty( "name" ), is( "Nancy" ) );
	        assertEquals(foundNode.getId(), n.getId() );
	        assertEquals((String) foundNode.getProperty( "name" ), "Nancy");
	        
	        // END SNIPPET: unitTest
	    }
}
