package eu.ehri.project.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.models.utils.EmptyIterable;

/**
 * Reindex a graph 
 *  
 * Should be part of SingleIndexGraphManager, so implicitly assumed it is compatible
 * 
 * @author paulboon
 *
 */
public class GraphReindexer {
    private static Logger logger = LoggerFactory.getLogger(GraphReindexer.class);
    public static final String INDEX_NAME = "entities";

    /**
     * recreate the index for all the Entity vertices
     */
    public static void reindex(FramedGraph<Neo4jGraph> graph) {
    	// clear the index
    	graph.getBaseGraph().dropIndex(INDEX_NAME);
    	Index<Vertex> index = graph.getBaseGraph().createIndex(INDEX_NAME, Vertex.class);
    	
    	// index vertices
    	for(Vertex vertex : graph.getVertices()) {
	        for (String key : propertyKeysToIndex(vertex)) {
	        	logger.debug("("+ key + ", "+ vertex.getProperty(key) + ")");
	            Object val = vertex.getProperty(key);
	            if (val != null)
	            	index.put(key, val, vertex);
	        }
    	};
    	
        graph.getBaseGraph().stopTransaction(
                TransactionalGraph.Conclusion.SUCCESS);
    }
    
    /*
    private Set<String> getPropertyKeysToIndex(Vertex vertex) {
		Set<String> keys = vertex.getPropertyKeys();
   		// quick test if we have an Entity
		if (keys.contains(EntityType.TYPE_KEY) && 
			keys.contains(EntityType.ID_KEY)) {
			// just index all
		} else {
			// no entity, index nothing
			keys = Collections.<String>emptySet();
		}
		return keys; 
    }
    */
    
    /**
     * collect the vertex property keys that exist in the Frame class with an @Property annotation
     * 
     * @param vertex
     * @return
     */
    private static Iterable<String> propertyKeysToIndex(Vertex vertex) {
    	String typeName = (String) vertex.getProperty(EntityType.TYPE_KEY);
    	try {
	    	EntityClass entityClass = EntityClass.withName(typeName); 
	    	Iterable<String> props = ClassUtils.getPropertyKeys(entityClass.getEntityClass());
	    	return props;
    	} catch (Exception e) {
    		return new EmptyIterable<String>();
    	}
    }
}
