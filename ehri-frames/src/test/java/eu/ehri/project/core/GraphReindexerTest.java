package eu.ehri.project.core;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class GraphReindexerTest extends ModelTestBase {
    private static Logger logger = LoggerFactory.getLogger(GraphReindexerTest.class);

    /**
     * Tests for this class rely on the index name defined here matching that used
     * internally by the default graph manager.
     */
    private static final String INDEX_NAME = "entities";

    @Test
    public void reindex() {
    	HashMap<EntityClass, Long> countBefore = countTypes();
        if (graph.getBaseGraph().getClass().isAssignableFrom(IndexableGraph.class)) {
            fail("Graph is not indexable: " + graph.getBaseGraph());
        }

    	new GraphReindexer(graph).reindex();
    	
    	// If the counts are the same, it is likely that we have all nodes indexed
    	assertTrue(countTypes().equals(countBefore));
    	
    	checkIndex();
    }
    
    /**
     * create a 'histogram' with the counts for each type of entity being indexed
	 *
     * @return the counts
     */
    private HashMap<EntityClass, Long> countTypes() {
    	HashMap<EntityClass, Long> counts = new HashMap<EntityClass, Long>();
    	Index<Vertex> index = ((IndexableGraph)graph.getBaseGraph()).getIndex(INDEX_NAME,
                Vertex.class);
    	EntityClass[] entityClasses = EntityClass.values();
        for (EntityClass entityClass : entityClasses) {
            counts.put(entityClass, index.count(EntityType.TYPE_KEY, entityClass));
            logger.debug("#" + entityClass + ": " + counts.get(entityClass));
        }
    	
    	return counts;
    }
    
    /**
     * check that each node is at least indexed by it's id
     */
    private void checkIndex() {
    	Index<Vertex> index = ((IndexableGraph)graph.getBaseGraph()).getIndex(INDEX_NAME,
                Vertex.class);
    	EntityClass[] entityClasses = EntityClass.values();
        for (EntityClass entityClass : entityClasses) {
            CloseableIterable<Vertex> vertices = index.get(EntityType.TYPE_KEY, entityClass);
            for (Vertex vertex : vertices) {
                String id = vertex.getProperty(EntityType.ID_KEY);
                assertEquals(vertex, index.get(EntityType.ID_KEY, id).iterator().next());
                logger.debug("id: " + id + " class: " + entityClass);
            }
            vertices.close();
        }
    }
}
