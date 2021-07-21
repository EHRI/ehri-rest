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

import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class CampsImporterTest extends AbstractImporterTest {
    private final String SKOS_FILE = "cvoc/camps.rdf";

    @Test
    public void testImportItems() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        assertNotNull(ios);

        SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary).importFile(ios, logMessage);

        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 8 more Concepts
       	 * 8 more ConceptDescription
	     * 9 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        int afterNodeCount = count + 26;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 8, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "675";
        QueryApi query = api(validUser).query();
        // Query for document identifier.
        List<Concept> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        assertEquals(1, toList(list.get(0).getBroaderConcepts()).size());

        // Check permission scopes
        for (Accessible e : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
    }

    @Test
    public void testImportItemsVersion2() throws Exception {

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        importer.importFile(ios, "Importing the camps as a SKOS file");

        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 8 more Concepts
       	 * 8 more ConceptDescription
     	 * 9 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        int afterNodeCount = count + 26;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 8, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "675";
        QueryApi query = api(validUser).query();
        // Query for document identifier.
        List<Concept> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        assertEquals(1, toList(list.get(0).getBroaderConcepts()).size());

        // Check permission scopes
        for (Accessible e : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }

        //import version 2
        String version2 = "cvoc/campsv02.rdf";
        ios = ClassLoader.getSystemResourceAsStream(version2);
        SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary)
            .allowUpdates(true)
            .importFile(ios, "Importing the modified camps as a SKOS file");

        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 0 new concepts, 2 modified concepts (counts as 0)
       	 * 0 new ConceptDescription, 2 modified (counts as 0)
         * 2 more import Event links (2 for every modified Unit, 1 for the User)
         * 1 more import Event
         */
        afterNodeCount = count + 26 + 3;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 8, toList(vocabulary.getConcepts()).size());
    }
}
