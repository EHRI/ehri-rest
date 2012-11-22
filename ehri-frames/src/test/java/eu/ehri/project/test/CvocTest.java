package eu.ehri.project.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;

public class CvocTest extends ModelTestBase {
   
	/**
	 * Just play a bit with a small 'graph' of concepts. 
	 * 
	 * NOTE: Better wait for the improved testing 'fixture' 
	 * before doing extensive testing on Concepts
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testConceptHierarchy() throws Exception {
		// Fruit, Apples and Bananas etc.
		
		GraphHelpers helpers = new GraphHelpers(this.graph.getBaseGraph().getRawGraph());
		helpers.createIndex(EntityTypes.CVOC_CONCEPT, Vertex.class);
		
	    Map<String, Object> data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "fruit");}};
		Vertex v_fruit = helpers.createIndexedVertex(data, EntityTypes.CVOC_CONCEPT);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "apples");}};
	    Vertex v_apples = helpers.createIndexedVertex(data, EntityTypes.CVOC_CONCEPT);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "bananas");}};
	    Vertex v_bananas = helpers.createIndexedVertex(data, EntityTypes.CVOC_CONCEPT);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "trees");}};
	    Vertex v_trees = helpers.createIndexedVertex(data, EntityTypes.CVOC_CONCEPT);

		// OK, so now we have fruit and more....
		// See if we can frame them
		Concept fruit = graph.frame(v_fruit, Concept.class);
	    Concept apples = graph.frame(v_apples, Concept.class);
	    Concept bananas = graph.frame(v_bananas, Concept.class);
	    Concept trees = graph.frame(v_trees, Concept.class);
	    
	    // OK, framed, now construct relations etc.
	    fruit.addNarrowerConcept(apples);
	    fruit.addNarrowerConcept(bananas);
	    
	    // TODO make assertions and no system outs !
	    
	    System.out.println("parent of " + apples.getIdentifier() + ": " + apples.getBroaderConcepts().iterator().next().getIdentifier());
	    System.out.println("parent of " + bananas.getIdentifier() + ": " + bananas.getBroaderConcepts().iterator().next().getIdentifier());
	    
	    // make  a relation to Trees concept
	    apples.addRelatedConcept(trees);
	    // is it symmetric?
	    Iterator<Concept> iterator = trees.getRelatedByConcepts().iterator(); 
	    if (iterator.hasNext()) {
	    	System.out.println("trees are also related to: " + iterator.next().getIdentifier());
	    }

	    // TODO test removal of a relation
	}
}
