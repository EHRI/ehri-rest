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

package eu.ehri.project.importers.cvoc;

import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class GhettosImporterV20140831Test extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int vocCount = toList(vocabulary.getConcepts()).size();
        SkosImporter importer;
        ImportLog log;
        try (InputStream ios = ClassLoader.getSystemResourceAsStream("cvoc/ghettos.rdf")) {
            importer = SkosImporterFactory.newSkosImporter(graph, adminUser, vocabulary)
                    .setTolerant(true)
                    .allowUpdates(true);
            log = importer.importFile(ios, logMessage);
        }

        /*  How many new nodes will have been created? We should have
         * 2 more Concepts
         * 5 more ConceptDescription
         * 5 UndeterminedRelationships
         * 3 more import Event links (2 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 16, getNodeCount(graph));
        assertEquals(2, log.getCreated());
        assertEquals(vocCount + 2, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "0";
        QueryApi query = api(adminUser).query();

        // Query for document identifier.
        List<Concept> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        Concept ghetto0 = manager.getEntity("cvoc1-0", Concept.class);
        //  <geo:lat>52.43333333333333</geo:lat>
        //	<geo:long>20.716666666666665</geo:long>
        assertEquals(Double.valueOf(52.43333333333333), ghetto0.getProperty("latitude"));
        assertEquals(Double.valueOf(20.716666666666665), ghetto0.getProperty("longitude"));

        int origCount;
        try (InputStream iosV2 = ClassLoader.getSystemResourceAsStream("cvoc/ghettos-v20140831.rdf.xml")) {
            origCount = getNodeCount(graph);
            importer.importFile(iosV2, logMessage);
        }

        /*
         * CREATED:
         * null: 3
         * cvocConceptDescription: 7
         * systemEvent: 1
         *
         * REMOVED:
         * relationship: 5
         * cvocConceptDescription: 5
         */
        assertEquals(origCount + (11-10), getNodeCount(graph));

        Concept ghetto0v2 = manager.getEntity("cvoc1-0", Concept.class);
        assertEquals(ghetto0, ghetto0v2);
        assertEquals(Double.valueOf(48.6666666667), ghetto0v2.getProperty("latitude"));
        assertEquals(Double.valueOf(26.5666666667), ghetto0v2.getProperty("longitude"));
    }
}
