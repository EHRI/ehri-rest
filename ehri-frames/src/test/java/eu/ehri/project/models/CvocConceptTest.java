package eu.ehri.project.models;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.test.ModelTestBase;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.CrudViews;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CvocConceptTest extends ModelTestBase {

    /**
     * Just play a bit with a small 'graph' of concepts.
     * 
     * NOTE: Better wait for the improved testing 'fixture' before doing
     * extensive testing on Concepts
     * 
     * @throws Exception
     */
    @SuppressWarnings("serial")
    @Test
    public void testConceptHierarchy() throws Exception {
        // Fruit, Apples and Bananas etc.

        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put(Ontology.IDENTIFIER_KEY, "fruit");
            }
        };
        Vertex v_fruit = manager.createVertex("fruit_id",
                EntityClass.CVOC_CONCEPT, data);

        data = new HashMap<String, Object>() {
            {
                put(Ontology.IDENTIFIER_KEY, "apples");
            }
        };
        Vertex v_apples = manager.createVertex("applies_id",
                EntityClass.CVOC_CONCEPT, data);

        data = new HashMap<String, Object>() {
            {
                put(Ontology.IDENTIFIER_KEY, "bananas");
            }
        };
        Vertex v_bananas = manager.createVertex("bananas_id",
                EntityClass.CVOC_CONCEPT, data);

        data = new HashMap<String, Object>() {
            {
                put(Ontology.IDENTIFIER_KEY, "trees");
            }
        };
        Vertex v_trees = manager.createVertex("trees_id",
                EntityClass.CVOC_CONCEPT, data);

        // OK, so now we have fruit and more....
        // See if we can frame them
        Concept fruit = graph.frame(v_fruit, Concept.class);
        Concept apples = graph.frame(v_apples, Concept.class);
        Concept bananas = graph.frame(v_bananas, Concept.class);
        Concept trees = graph.frame(v_trees, Concept.class);

        // OK, framed, now construct relations etc.
        fruit.addNarrowerConcept(apples);
        fruit.addNarrowerConcept(bananas);
        graph.getBaseGraph().commit();

        // fruit should now be the broader concept
        assertEquals(fruit.getId(), apples.getBroaderConcepts()
                .iterator().next().getId());
        assertEquals(fruit.getId(), bananas.getBroaderConcepts()
                .iterator().next().getId());

        // make a relation to Trees concept
        apples.addRelatedConcept(trees);
        graph.getBaseGraph().commit();

        // is it symmetric?
        assertEquals(apples.getId(), trees.getRelatedByConcepts()
                .iterator().next().getId());

        // TODO test removal of a relation
    }

    private String TEST_LABEL_LANG = "en-US";

    // @formatter:off
    @SuppressWarnings("serial")
    protected Map<String, Object> getAppleTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return new HashMap<String, Object>() {{
            put(Bundle.ID_KEY, null);
            put(Bundle.TYPE_KEY, Entities.CVOC_CONCEPT);
            put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                put(Ontology.IDENTIFIER_KEY, "apple");
            }});
            put(Bundle.REL_KEY, new HashMap<String, Object>() {{
                put("describes", new LinkedList<HashMap<String, Object>>() {{
                    add(new HashMap<String, Object>() {{
                        //put(Bundle.ID_KEY, "cvd1");
                        put(Bundle.TYPE_KEY, Entities.CVOC_CONCEPT_DESCRIPTION);
                        put(Bundle.DATA_KEY, new HashMap<String, Object>() {{
                            put(Ontology.LANGUAGE_OF_DESCRIPTION, TEST_LABEL_LANG);
                            put(Ontology.PREFLABEL, "pref1");
                            // other properties are optional, but we put them in
                            put("altLabel", Lists.<String>newArrayList("alt1", "alt2"));
                            put(Ontology.CONCEPT_DEFINITION, Lists.<String>newArrayList("def1")); // allow multiple
                            put(Ontology.CONCEPT_SCOPENOTE, Lists.<String>newArrayList("sn1")); // allow multiple
                        }});
                    }});
                }});
            }});
        }};
    }
    // @formatter:on

    @Test
    public void testCreateConceptWithDescription() throws Exception {
        UserProfile validUser = manager.getFrame("mike", UserProfile.class);
        Crud<Concept> conceptViews = new CrudViews<Concept>(graph,
                Concept.class);
        Bundle bundle = Bundle.fromData(getAppleTestBundle());

        Concept concept = null;
        concept = conceptViews.create(bundle, validUser);
        graph.getBaseGraph().commit();

		// Does the label have the correct properties
		assertNotNull(concept);
		
		// test for description
		Description description = concept.getDescriptions().iterator().next();
		assertEquals(TEST_LABEL_LANG, description.getLanguageOfDescription());
		
		//String[] altLabels = ((ConceptDescription)description).getAltLabels();		
		// NOTE: use framing on the vertex to get the Model class
		// that is the frames way of doning things
		
		ConceptDescription descr = graph.frame(description.asVertex(), ConceptDescription.class);		
		assertEquals("pref1", descr.getPrefLabel());
		// etc. etc.
		
		//String[] altLabels = descr.getAltLabels();		
		//assertEquals("alt2", altLabels[1]);
		// NOTE we can't call getAltLabels() on the interface, because it is optional
		List<String> altLabels = descr.asVertex().getProperty(Ontology.CONCEPT_ALTLABEL);
		assertFalse(altLabels == null);
		assertEquals(2, altLabels.size());
		assertEquals("alt2", altLabels.get(1));
	}
    
	@SuppressWarnings("serial")
    @Test
	public void testAddConceptToVocabulary() throws Exception {
		UserProfile validUser = manager.getFrame("mike", UserProfile.class);

		Map<String, Object> data = new HashMap<String, Object>() {{put(Ontology.IDENTIFIER_KEY, "testVocabulary");}};
		Vertex v_voc = manager.createVertex("voc_id", EntityClass.CVOC_VOCABULARY, data);
		data = new HashMap<String, Object>() {{put(Ontology.IDENTIFIER_KEY, "apples");}};
		Vertex v_apples = manager.createVertex("applies_id", EntityClass.CVOC_CONCEPT, data);

		// frame it
		Vocabulary vocabulary = graph.frame(v_voc, Vocabulary.class);
		Concept apples = graph.frame(v_apples, Concept.class);

		// now add the apples to the vocabulary
        vocabulary.addItem(apples);
        assertEquals(vocabulary.getIdentifier(), apples.getVocabulary().getIdentifier());
	}
	
	// test creation of a vocabulary using the BundleDAO
	@Test
	public void testCreateVocabulary() throws Exception {
        String vocid = "voc-test-id";
        Bundle bundle = Bundle.Builder.withClass(EntityClass.CVOC_VOCABULARY)
                .setId(vocid)
                .addDataValue(Ontology.IDENTIFIER_KEY, vocid)
                .addDataValue(Ontology.NAME_KEY, "Test Vocabulaey")
                .build();
         Vocabulary vocabulary = new BundleDAO(graph).create(bundle, Vocabulary.class);
         assertEquals(vocid, vocabulary.getIdentifier());
	}
}
