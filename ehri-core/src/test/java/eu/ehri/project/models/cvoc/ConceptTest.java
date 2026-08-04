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

package eu.ehri.project.models.cvoc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


public class ConceptTest extends AbstractFixtureTest {

    private static final String TEST_LABEL_LANG = "en-US";

    private Vocabulary v;
    private Concept cv1;
    private Concept cv2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        v = manager.getEntity("cvoc1", Vocabulary.class);
        cv1 = manager.getEntity("cvocc1", Concept.class);
        cv2 = manager.getEntity("cvocc2", Concept.class);
    }

    @Test
    public void testGetVocabulary() throws Exception {
        assertEquals(v, cv1.getVocabulary());
    }

    @Test
    public void testGetBroaderConcepts() throws Exception {
        assertEquals(cv1, cv2.getBroaderConcepts().iterator().next());
    }

    @Test
    public void testGetNarrowerConcepts() throws Exception {
        assertEquals(cv2, cv1.getNarrowerConcepts().iterator().next());
    }

    @Test
    public void testRemoveNarrowerConcept() throws Exception {
        cv1.removeNarrowerConcept(cv2);
        assertFalse(cv2.getNarrowerConcepts().iterator().hasNext());
    }

    @Test
    public void testGetRelatedConcepts() throws Exception {
        assertEquals(cv1, cv2.getRelatedConcepts().iterator().next());
        assertEquals(cv2, cv1.getRelatedByConcepts().iterator().next());
    }

    @Test
    public void testRemoveBroaderConcept() throws Exception {
        cv2.removeBroaderConcept(cv1);
        assertFalse(cv2.getBroaderConcepts().iterator().hasNext());
    }

    @Test
    public void testRemoveRelatedConcept() throws Exception {
        cv2.removeRelatedConcept(cv1);
        assertFalse(cv2.getRelatedConcepts().iterator().hasNext());
    }

    @Test
    public void testConceptHierarchy() throws Exception {
        // Fruit, Apples and Bananas etc.
        Vertex fruitVertex = manager.createVertex(
                "fruit_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "fruit",
                        Ontology.PID_KEY, "fruit-1234"
                )
        );
        Vertex applesVertex = manager.createVertex(
                "apples_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "apples",
                        Ontology.PID_KEY, "apples-1234"
                )
        );
        Vertex bananasVertex = manager.createVertex(
                "bananas_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "bananas",
                        Ontology.PID_KEY, "bananas-1234"
                )
        );
        Vertex treesVertex = manager.createVertex(
                "trees_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "trees",
                        Ontology.PID_KEY, "trees-1234"
                )
        );

        // See if we can frame them
        Concept fruit = graph.frame(fruitVertex, Concept.class);
        Concept apples = graph.frame(applesVertex, Concept.class);
        Concept bananas = graph.frame(bananasVertex, Concept.class);
        Concept trees = graph.frame(treesVertex, Concept.class);

        // Now construct relations etc.
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
    }

    private Map<String, Object> getAppleTestBundle() {
        // Data structure representing a not-yet-created collection.
        // Using double-brace initialization to ease the pain.
        return ImmutableMap.of(
                // Note: Bundle.ID_KEY omitted as ImmutableMap does not allow null values
                Bundle.TYPE_KEY, Entities.CVOC_CONCEPT,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "apple",
                        Ontology.PID_KEY, "apple-1234"
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

        Description description = concept.getDescriptions().iterator().next();
        assertEquals(TEST_LABEL_LANG, description.getLanguageOfDescription());

        // NOTE: use framing on the vertex to get the Model class
        // that is the frames way of doing things
        ConceptDescription descr = graph.frame(description.asVertex(), ConceptDescription.class);
        assertEquals("pref1", descr.getName());

        // NOTE we can't call getAltLabels() on the interface, because it is optional
        List<String> altLabels = descr.getProperty("altLabel");
        assertNotNull(altLabels);
        assertEquals(2, altLabels.size());
        assertEquals("alt2", altLabels.get(1));
    }

    @Test
    public void testAddConceptToVocabulary() throws Exception {
        Vertex vocabularyVertex = manager.createVertex(
                "voc_id",
                EntityClass.CVOC_VOCABULARY,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "testVocabulary",
                        Ontology.PID_KEY, "testVocabulary-1234"
                )
        );
        Vertex applesVertex = manager.createVertex(
                "apples_id",
                EntityClass.CVOC_CONCEPT,
                ImmutableMap.of(
                        Ontology.IDENTIFIER_KEY, "apples",
                        Ontology.PID_KEY, "apples-1234"
                )
        );

        Vocabulary vocabulary = graph.frame(vocabularyVertex, Vocabulary.class);
        Concept apples = graph.frame(applesVertex, Concept.class);

        vocabulary.addItem(apples);
        assertEquals(vocabulary.getIdentifier(), apples.getVocabulary().getIdentifier());
    }

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
