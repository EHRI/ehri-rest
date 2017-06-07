/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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

package eu.ehri.project.importers.eac;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class PersonalitiesV2Test extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesV2Test.class);

    private static final String PROPERTIES = "personalitiesV2.properties";

    @Test
    public void newPersonalitiesWithoutCreatedBy() throws Exception {
        final String file = "PersonalitiesV2withoutCreatedBy.xml";
        final String logMessage = "Importing EAC " + file;
        InputStream ios = ClassLoader.getSystemResourceAsStream(file);
        int count = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        saxImportManager(EacImporter.class, EacHandler.class, PROPERTIES)
                .withScope(SystemScope.getInstance())
                .importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        /*
         * null: 2
         * relationship: 1
         * historicalAgent: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * historicalAgentDescription: 1
         */
        assertEquals(count + 7, getNodeCount(graph));
        printGraph(graph);
        HistoricalAgent person = manager.getEntity("ehri_pers_000051", HistoricalAgent.class);
        assertEquals(2, ((List) person.getProperty(Ontology.OTHER_IDENTIFIERS)).size());

        for (Description d : person.getDescriptions()) {
            assertFalse(d.getPropertyKeys().contains(Ontology.OTHER_IDENTIFIERS));
            assertEquals("deu", d.getLanguageOfDescription());
            assertEquals("Booooris the third", d.getName());
            assertTrue(d.getProperty("otherFormsOfName") instanceof List);
            assertEquals(2, ((List) d.getProperty("otherFormsOfName")).size());
            assertTrue(d.getProperty("place") instanceof List);
            assertEquals(2, ((List) d.getProperty("place")).size());
        }
    }

    @Test
    public void newPersonalitiesWithoutReferredNodes() throws Exception {
        final String SINGLE_EAC = "PersonalitiesV2.xml";
        final String logMessage = "Importing EAC " + SINGLE_EAC;
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
        int count = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        saxImportManager(EacImporter.class, EacHandler.class, PROPERTIES)
                .withScope(SystemScope.getInstance())
                .importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        /*
         * null: 2
         * relationship: 1
         * historicalAgent: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * historicalAgentDescription: 1
         */
        assertEquals(count + 7, getNodeCount(graph));
        printGraph(graph);
        HistoricalAgent person = manager.getEntity("ehri_pers_000051", HistoricalAgent.class);
        for (Description d : person.getDescriptions()) {
            assertEquals("deu", d.getLanguageOfDescription());
            assertTrue(d.getProperty("otherFormsOfName") instanceof List);
            assertEquals(2, ((List) d.getProperty("otherFormsOfName")).size());
            assertTrue(d.getProperty("place") instanceof List);
            assertEquals(2, ((List) d.getProperty("place")).size());
        }
    }

    @Test
    public void newPersonalitiesWithReferredNodes() throws Exception {
        Bundle vocabularyBundle = Bundle.of(EntityClass.CVOC_VOCABULARY)
                .withDataValue(Ontology.IDENTIFIER_KEY, "FAST_keywords")
                .withDataValue(Ontology.NAME_KEY, "FAST Keywords");
        Bundle conceptBundle = Bundle.of(EntityClass.CVOC_CONCEPT)
                .withDataValue(Ontology.IDENTIFIER_KEY, "fst894382");
        Vocabulary vocabulary = api(validUser).create(vocabularyBundle, Vocabulary.class);
        logger.debug(vocabulary.getId());
        Concept concept_716 = api(validUser).create(conceptBundle, Concept.class);
        vocabulary.addItem(concept_716);


        Vocabulary vocabularyTest = manager.getEntity("fast_keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);


        final String SINGLE_EAC = "PersonalitiesV2.xml";
        final String logMessage = "Importing EAC " + SINGLE_EAC;
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
        int count = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        saxImportManager(EacImporter.class, EacHandler.class, PROPERTIES)
                .withScope(SystemScope.getInstance())
                .importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        /*
         * null: 2
         * relationship: 1
         * historicalAgent: 1
         * link: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * historicalAgentDescription: 1
         */
        assertEquals(count + 8, getNodeCount(graph));

        HistoricalAgent person = manager.getEntity("ehri_pers_000051", HistoricalAgent.class);
        for (Description d : person.getDescriptions()) {
            for (AccessPoint rel : d.getAccessPoints()) {
                if (rel.getRelationshipType().equals(AccessPointType.subject)) {
                    if (rel.getName().equals("Diplomatic documents")) {
                        assertEquals(1, toList(rel.getLinks()).size());
                        for (Link link : rel.getLinks()) {
                            boolean conceptFound = false;
                            for (Linkable le : link.getLinkTargets()) {
                                if (le.getType().equals("CvocConcept")) {
                                    assertEquals(le, concept_716);
                                    conceptFound = true;
                                }
                            }
                            assertTrue(conceptFound);
                            logger.debug(link.getLinkType());
                            for (String key : link.getPropertyKeys()) {
                                logger.debug(key + ":" + link.getProperty(key));
                            }
                        }
                    }
                }
            }
        }
    }
}
