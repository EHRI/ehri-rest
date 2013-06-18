package eu.ehri.extension;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.PluginLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// in start do a one time 'initialization' like setting Handlers
public class EhriInitializer implements PluginLifecycle {
	   @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory
	            .getLogger(EhriInitializer.class);

	@Override
	public Collection<Injectable<?>> start(GraphDatabaseService service,
			Configuration conf) {
		
		//logger.info("========= EhriInitializer start called");
		System.out.println("========= EhriInitializer start called");
		
		/* TODO regsiter handler when we want to use it, but leave it disabled for now!
		EhriTxEventHandler handler = new EhriTxEventHandler(); 
		service.registerTransactionEventHandler( handler );
		*/
		
		// return an empty list
		Collection<Injectable<?>> result = new ArrayList<Injectable<?>>();
		return result;
	}

	@Override
	public void stop() {
		//logger.info("========= EhriInitializer stop called");
		System.out.println("========= EhriInitializer stop called");
	}

	// detect changes on the entity's that important for ehri search indexing
	private static class EhriTxEventHandler implements TransactionEventHandler<Object>
    {
       
        public void afterCommit( TransactionData data, Object state )
        {
        	//System.out.println("========= EhriTxEventHandler afterCommit called");
        	
        	// direct Node changes
        	
        	for ( PropertyEntry<Node> entry : data.assignedNodeProperties() ) {
        		String key = entry.key();
        		Object value = entry.value();    
        		// assume all our stuff has an __ID__ and if it is assigned the node is created
        		if ("__ID__".equals(key)) {
        			//System.out.println("========= created " + value);
        		} else {
        			if (entry.entity().hasProperty("__ID__")) {
        				// change of the entity
        				//System.out.println("========= changed " + entry.entity().getProperty("__ID__"));
        			}
        		}
        	}

        	for ( PropertyEntry<Node> entry : data.removedNodeProperties() ) {
        		String key = entry.key();
        		Object value = entry.previouslyCommitedValue();    
        		// assume all our stuff has an __ID__ and if it's removed the node is deleted
        		if ("__ID__".equals(key)) {
        			//System.out.println("========= deleted " + value);
        		}
        	}
        	
        	// Relationship changes
        	// when changed, affect Nodes on both sides 
        	// But it seems that it is onlty a change of the property and not the relation itself
        	// So we can skip those
        	// TODO check
        	/*
        	for ( PropertyEntry<Relationship> entry : data.assignedRelationshipProperties() ) {
        		for (Node node: entry.entity().getNodes()) {
            		if (node.hasProperty("__ID__")) {
            			System.out.println("========= node with changed relation " + node.getProperty("__ID__"));
            		}        			
        		}        		
        	}

        	for ( PropertyEntry<Relationship> entry : data.removedRelationshipProperties() )
        	{
        		for (Node node: entry.entity().getNodes()) {
            		if (node.hasProperty("__ID__")) {
            			System.out.println("========= node with changed relation " + node.getProperty("__ID__"));
            		}        			
        		}
        	}
        	*/
        }

        public void afterRollback( TransactionData data, Object state )
        {
        }

        public Object beforeCommit( TransactionData data )
                throws Exception
        {
            return null;
        }
    }	
}
