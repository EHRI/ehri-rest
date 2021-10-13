/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class JoodsRaadTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(JoodsRaadTest.class);
    protected final String EHRI_SKOS_TERM = "cvoc/joods_raad.xml";
    protected final String NIOD_SKOS_TERM = "cvoc/niod-joodseraad.xml";

    @Test
    public void testRelatedWithinScheme() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        assertNotNull(ios);

        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        importer.importFile(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        /*  How many new nodes will have been created? We should have
         * 5 more Concepts
       	 * 9 more ConceptDescription
     	 * 6 more import Event links
         * 1 more import Event
         */
        assertEquals(count + 21, getNodeCount(graph));
        assertEquals(voccount + 5, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "698";
        QueryApi query = api(validUser).query();
        // Query for document identifier.
        List<Concept> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        assertEquals(1, toList(list.get(0).getBroaderConcepts()).size());

        // Check permission scopes
        for (Accessible e : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
        Concept term698 = manager.getEntity("cvoc1-698", Concept.class);
        boolean found = false;
        for (Concept rel : term698.getRelatedConcepts()) {
            found = true;
            assertEquals("307", rel.getIdentifier());
        }
        assertTrue(found);
    }

    @Test
    public void testCloseMatchOutsideScheme() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary cvoc1 = manager.getEntity("cvoc1", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, cvoc1)
                .setTolerant(true);
        importer.importFile(ios, logMessage);


        int count = getNodeCount(graph);
        assertNotNull(ios);

        Vocabulary cvoc2 = manager.getEntity("cvoc2", Vocabulary.class);
        InputStream niod_ios = ClassLoader.getSystemResourceAsStream(NIOD_SKOS_TERM);
        assertNotNull(niod_ios);
        SkosImporter niod_importer = SkosImporterFactory.newSkosImporter(graph, validUser, cvoc2);
        niod_importer.setTolerant(true);
        int voccount = toList(cvoc2.getConcepts()).size();

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        niod_importer.importFile(niod_ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        /*  How many new nodes will have been created? We should have
         * 1 more Concepts
       	 * 1 more ConceptDescription
         * 1 more Link
	 * 2 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 6, getNodeCount(graph));
        assertEquals(voccount + 1, toList(cvoc2.getConcepts()).size());

        Concept term698 = manager.getEntity("cvoc1-698", Concept.class);
        boolean found = false;
        for (Concept rel : term698.getRelatedConcepts()) {
            found = true;
            assertEquals("307", rel.getIdentifier());
        }
        assertTrue(found);

        Concept termJR = manager.getEntity("cvoc2-joodse_raad", Concept.class);

        for (Link desc : termJR.getLinks()) {
            assertTrue(desc.getPropertyKeys().contains("type"));
            assertEquals("associative", desc.getProperty("type"));
            assertTrue(desc.getPropertyKeys().contains("skos"));
            assertEquals("exactMatch", desc.getProperty("skos"));
        }

        Concept concept698 = manager.getEntity("cvoc1-698", Concept.class);
        found = false;
        for (Edge e : concept698.asVertex().getEdges(Direction.IN, "hasLinkTarget")) {
            Link l = graph.frame(e.getVertex(Direction.OUT), Link.class);
            boolean bothTargetsFound = false;
            for (Linkable entity : l.getLinkTargets()) {
                if (entity.equals(termJR))
                    bothTargetsFound = true;
            }
            assertTrue(bothTargetsFound);
            for (String k : e.getVertex(Direction.OUT).getPropertyKeys()) {
                logger.debug(k + ":" + e.getVertex(Direction.OUT).getProperty(k));
            }
            int countHasLinkTarget = 0;
            for (Edge out : e.getVertex(Direction.OUT).getEdges(Direction.OUT)) {
                logger.debug(out.getLabel());
                if (out.getLabel().equals("hasLinkTarget")) {
                    countHasLinkTarget++;
                }
            }
            assertEquals(2, countHasLinkTarget);
            found = true;
        }
        assertTrue(found);

        found = false;
        for (Concept rel : termJR.getRelatedConcepts()) {
            for (String key : rel.getPropertyKeys()) {
                logger.debug(key + "" + rel.getProperty(key));
            }
            found = true;
        }
        assertFalse(found);
    }
}
