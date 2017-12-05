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

package eu.ehri.project.importers.cvoc;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class EventsSkosImporterTest extends AbstractImporterTest {

    private final String EVENT_SKOS = "cvoc/ehri-events.rdf";
    private final String EHRI_SKOS_TERM = "cvoc/joods_raad.xml";
    final String logMessage = "Importing a single skos: " + EVENT_SKOS;

    @Test
    public void importAllEvents() throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream("cvoc/allEhriEvents.rdf");
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        importer.importFile(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
       /*
        * relationship: 5
        * null: 2
        * cvocConceptDescription: 1
        * systemEvent: 1
        * cvocConcept: 1 
        */
        printGraph(graph);

    }

    @Test
    public void testImportItems() throws Exception {

        int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EVENT_SKOS);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary)
                .setTolerant(true);
        //auths
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        importer.importFile(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        /* How many new nodes will have been created? We should have
         * relationship: 4
         * null: 3
         * link: 1
         * cvocConceptDescription: 2
         * systemEvent: 1
         * cvocConcept: 2
         */
        assertEquals(count + 13, getNodeCount(graph));
        printGraph(graph);


        Concept bloodForGoods = manager.getEntity("cvoc1-1", Concept.class);
        for (Description desc : bloodForGoods.getDescriptions()) {
            assertTrue(desc.getPropertyKeys().contains("scopeNote"));
        }
        Concept teheranChildren = manager.getEntity("cvoc1-2", Concept.class);
        assertTrue(teheranChildren.getPropertyKeys().contains(AccessPointType.person.name()));

        boolean found = false;
        for (Link desc : teheranChildren.getLinks()) {
            found = true;
            assertTrue(desc.getPropertyKeys().contains("type"));
            assertEquals("associative", desc.getProperty("type"));
            assertTrue(desc.getPropertyKeys().contains("sem"));
            assertEquals("person", desc.getProperty("sem"));
            for (Linkable e : desc.getLinkTargets()) {
                assertTrue(e.getId().equals("cvoc1-2") || e.getId().equals("a1"));
            }
        }
        assertTrue(found);

    }

    @Test
    public void withOutsideScheme() throws ItemNotFound, IOException, InputParseError, ValidationError {
        Vocabulary cvoc1 = manager.getEntity("cvoc1", Vocabulary.class);
        InputStream ios1 = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        SkosImporterFactory.newSkosImporter(graph, validUser, cvoc1)
                .setTolerant(true)
                .importFile(ios1, logMessage);

        int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getEntity("cvoc2", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EVENT_SKOS);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        importer.importFile(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        /* How many new nodes will have been created? We should have
         * link: 3
         * relationship: 2
         * null: 3
         * cvocConceptDescription: 2
         * systemEvent: 1
         * cvocConcept: 2
         */
        assertEquals(count + 13, getNodeCount(graph));

        Concept termJR = manager.getEntity("cvoc1-tema_866", Concept.class);

        boolean found = false;
        for (Link desc : termJR.getLinks()) {
            found = true;
            assertTrue(desc.getPropertyKeys().contains("type"));
            assertEquals("associative", desc.getProperty("type"));
            assertTrue(desc.getPropertyKeys().contains("skos"));
            assertTrue(desc.getProperty("skos").equals("broadMatch") || desc.getProperty("skos").equals("relatedMatch"));
        }
        assertTrue(found);

    }
}
