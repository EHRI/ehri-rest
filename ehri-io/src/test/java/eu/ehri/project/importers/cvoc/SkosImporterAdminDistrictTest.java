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

import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;


public class SkosImporterAdminDistrictTest extends AbstractImporterTest {

    protected final String SINGLE_SKOS = "cvoc/admin-dist-nolang.rdf";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
  
    @Test
    public void testImportItems() throws Exception {
        final String logMessage = "Importing a single skos: " + SINGLE_SKOS;

        int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_SKOS)) {
            SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, adminUser, vocabulary).setTolerant(true);
            importer.importFile(ios, logMessage);
        }

        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 3 more Concept
        // - 5 more ConceptDescription ( 3 de + 1 fr + 1 eng (:when no lang is given in the prefLabel:) )
        // - 4 more ImportEvents ( 3 + 1 )
        // - 1 more import Action
        assertEquals(count + 13, getNodeCount(graph));

    }
}
