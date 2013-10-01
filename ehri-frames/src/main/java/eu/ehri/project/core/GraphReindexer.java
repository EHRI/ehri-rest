package eu.ehri.project.core;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.models.utils.EmptyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reindex a graph 
 *  
 * Should be part of SingleIndexGraphManager, so implicitly assumed it is compatible
 * 
 * @author paulboon
 *
 */
public class GraphReindexer<T extends TransactionalGraph & IndexableGraph> {
    private static Logger logger = LoggerFactory.getLogger(GraphReindexer.class);
    public static final String INDEX_NAME = "entities";

    private final FramedGraph<T> graph ;

    public GraphReindexer(FramedGraph<T> graph) {
        this.graph = graph;
    }

    /**
     * recreate the index for all the Entity vertices
     */
    public void reindex(String indexName) {
    	// clear the index
        try {
            graph.getBaseGraph().dropIndex(indexName);
            Index<Vertex> index = graph.getBaseGraph().createIndex(indexName, Vertex.class);

            // index vertices
            for(Vertex vertex : graph.getVertices()) {
                for (String key : propertyKeysToIndex(vertex)) {
                    logger.debug("("+ key + ", "+ vertex.getProperty(key) + ")");
                    Object val = vertex.getProperty(key);
                    if (val != null) {
                        index.put(key, val, vertex);
                    }
                }
            }

            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
        }
    }
    
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
