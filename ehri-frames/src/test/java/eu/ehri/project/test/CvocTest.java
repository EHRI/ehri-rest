package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Text;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.CrudViews;

public class CvocTest extends ModelTestBase {
   	
	/**
	 * Just play a bit with a small 'graph' of concepts. 
	 * 
	 * NOTE: Better wait for the improved testing 'fixture' 
	 * before doing extensive testing on Concepts
	 * 
	 * @throws Exception 
	 */
	@SuppressWarnings("serial")
	@Test
	public void testConceptHierarchy() throws Exception {
		// Fruit, Apples and Bananas etc.
				
	    Map<String, Object> data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "fruit");}};
	    Vertex v_fruit = manager.createVertex("fruit_id", EntityClass.CVOC_CONCEPT, data);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "apples");}};
	    Vertex v_apples = manager.createVertex("applies_id", EntityClass.CVOC_CONCEPT, data);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "bananas");}};
	    Vertex v_bananas = manager.createVertex("bananas_id", EntityClass.CVOC_CONCEPT, data);

	    data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "trees");}};
	    Vertex v_trees = manager.createVertex("trees_id", EntityClass.CVOC_CONCEPT, data);

		// OK, so now we have fruit and more....
		// See if we can frame them
		Concept fruit = graph.frame(v_fruit, Concept.class);
		Concept apples = graph.frame(v_apples, Concept.class);
		Concept bananas = graph.frame(v_bananas, Concept.class);
		Concept trees = graph.frame(v_trees, Concept.class);

		// OK, framed, now construct relations etc.
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			fruit.addNarrowerConcept(apples);
			fruit.addNarrowerConcept(bananas);
			tx.success();
		} catch (Exception e) {
			tx.failure();
		} finally {
			tx.finish();
		}

		// fruit should now be the broader concept
		assertEquals(fruit.getIdentifier(), apples.getBroaderConcepts().iterator().next().getIdentifier());
		assertEquals(fruit.getIdentifier(), bananas.getBroaderConcepts().iterator().next().getIdentifier());

		// make a relation to Trees concept
		tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			apples.addRelatedConcept(trees);
			tx.success();
		} catch (Exception e) {
			tx.failure();
		} finally {
			tx.finish();
		}

		// is it symmetric?
		assertEquals(apples.getIdentifier(), trees.getRelatedByConcepts().iterator().next().getIdentifier());

		// TODO test removal of a relation
	}
	
	private String TEST_LABEL_CONTENT = "apple-label-content";
	private String TEST_LABEL_LANG =  "en-US";
	// @formatter:off
    @SuppressWarnings("serial")
    protected Map<String, Object> getAppleTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.ID_KEY, null);
            put(Bundle.TYPE_KEY, Entities.CVOC_CONCEPT);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(AccessibleEntity.IDENTIFIER_KEY, "apple");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put("prefLabel", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        put(Bundle.ID_KEY, null);
                        put(Bundle.TYPE_KEY, Entities.CVOC_TEXT);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(AccessibleEntity.IDENTIFIER_KEY, "apple-someid");
                            put(Text.LANGUAGE, TEST_LABEL_LANG);
                            put(Text.CONTENT, TEST_LABEL_CONTENT);
                        }});
                    }});
                }});
            }});
        }};
    }
    // @formatter:on
    
	@Test
	public void testCreateConceptWithLabel() throws Exception {
		UserProfile validUser = manager.getFrame("mike", UserProfile.class);
        Crud<Concept> conceptViews = new CrudViews<Concept>(graph,
        		Concept.class);
        Map<String, Object> bundle = getAppleTestBundle();
 
        Concept concept = null;
        concept = conceptViews.create(bundle, validUser);
        graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);

		// Does the label have the correct properties
		assertNotNull(concept);
		assertEquals(TEST_LABEL_CONTENT, concept.getPrefLabel().iterator().next().getContent());
		assertEquals(TEST_LABEL_LANG, concept.getPrefLabel().iterator().next().getLanguage());
	}
    
	@Test
	public void testAddConceptToVocabulary() throws Exception {
		UserProfile validUser = manager.getFrame("mike", UserProfile.class);

		Map<String, Object> data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "testVocabulary");}};
		Vertex v_voc = manager.createVertex("voc_id", EntityClass.CVOC_VOCABULARY, data);
		data = new HashMap<String, Object>() {{put(AccessibleEntity.IDENTIFIER_KEY, "apples");}};
		Vertex v_apples = manager.createVertex("applies_id", EntityClass.CVOC_CONCEPT, data);

		// frame it
		Vocabulary vocabulary = graph.frame(v_voc, Vocabulary.class);
		Concept apples = graph.frame(v_apples, Concept.class);

		// now add the apples to the vocabulary
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			vocabulary.addConcept(apples);
			tx.success();
		} catch (Exception e) {
			tx.failure();
		} finally {
			tx.finish();
		}

		assertEquals(vocabulary.getIdentifier(), apples.getVocabulary().getIdentifier());

	}
}
