/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CvocConceptTest extends ModelTestBase {

    /**
     * Just play a bit with a small 'graph' of concepts.
     * <p>
     * NOTE: Better wait for the improved testing 'fixture' before doing
     * extensive testing on Concepts
     */
    @Test
    public void testConceptHierarchy() throws Exception {
        // Fruit, Apples and Bananas etc.
        Vertex v_fruit = manager.createVertex(
                "fruit_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "fruit")
        );
        Vertex v_apples = manager.createVertex(
                "applies_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "apples")
        );
        Vertex v_bananas = manager.createVertex(
                "bananas_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "bananas")
        );
        Vertex v_trees = manager.createVertex(
                "trees_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "trees")
        );

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

    private final String TEST_LABEL_LANG = "en-US";

    protected Map<String, Object> getAppleTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return ImmutableMap.of(
                // Note: Bundle.ID_KEY omitted as ImmutableMap does not allow null values
                Bundle.TYPE_KEY, Entities.CVOC_CONCEPT,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "apple"
                ),
                Bundle.REL_KEY, ImmutableMap.of(
                        "describes", ImmutableList.of(
                                ImmutableMap.of(
                                        Bundle.TYPE_KEY, Entities.CVOC_CONCEPT_DESCRIPTION,
                                        Bundle.DATA_KEY, ImmutableMap.of(
                                                Ontology.LANGUAGE_OF_DESCRIPTION, TEST_LABEL_LANG,
                                                Ontology.PREFLABEL, "pref1",
                                                "altLabel", ImmutableList.of("alt1", "alt2"),
                                                "definition", ImmutableList.of("def1"),
                                                "scopeNote", ImmutableList.of("sn1")
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void testCreateConceptWithDescription() throws Exception {
        UserProfile validUser = manager.getEntity("mike", UserProfile.class);
        Bundle bundle = Bundle.fromData(getAppleTestBundle());

        Concept concept = api(validUser).create(bundle, Concept.class);
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
        assertEquals("pref1", descr.getName());
        // etc. etc.

        //String[] altLabels = descr.getAltLabels();
        //assertEquals("alt2", altLabels[1]);
        // NOTE we can't call getAltLabels() on the interface, because it is optional
        List<String> altLabels = descr.getProperty("altLabel");
        assertNotNull(altLabels);
        assertEquals(2, altLabels.size());
        assertEquals("alt2", altLabels.get(1));
    }

    @Test
    public void testAddConceptToVocabulary() throws Exception {
        Vertex v_voc = manager.createVertex(
                "voc_id",
                EntityClass.CVOC_VOCABULARY,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "testVocabulary")
        );
        Vertex v_apples = manager.createVertex(
                "applies_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(Ontology.IDENTIFIER_KEY, "apples")
        );

        // frame it
        Vocabulary vocabulary = graph.frame(v_voc, Vocabulary.class);
        Concept apples = graph.frame(v_apples, Concept.class);

        // now add the apples to the vocabulary
        vocabulary.addItem(apples);
        assertEquals(vocabulary.getIdentifier(), apples.getVocabulary().getIdentifier());
    }

    // test creation of a vocabulary using the BundleManager
    @Test
    public void testCreateVocabulary() throws Exception {
        String vocid = "voc-test-id";
        Bundle bundle = Bundle.Builder.withClass(EntityClass.CVOC_VOCABULARY)
                .setId(vocid)
                .addDataValue(Ontology.IDENTIFIER_KEY, vocid)
                .addDataValue(Ontology.PID_KEY, "pid-" + vocid)
                .addDataValue(Ontology.NAME_KEY, "Test Vocabulary")
                .build();
        Vocabulary vocabulary = new BundleManager(graph).create(bundle, Vocabulary.class);
        assertEquals(vocid, vocabulary.getIdentifier());
    }
}
