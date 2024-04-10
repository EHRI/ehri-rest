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

package eu.ehri.project.importers.csv;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class CsvDossinImporterTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItems() throws Exception {

        Repository ps = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing some Dossin records";

        int count = getNodeCount(graph);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        try (InputStream ios = ClassLoader.getSystemResourceAsStream("dossin.csv")) {
            ImportLog importLog = CsvImportManager.create(graph, ps, adminUser, EadImporter.class, ImportOptions.basic())
                    .importInputStream(ios, logMessage);
            System.out.println(importLog);
            // After...
            List<VertexProxy> graphState2 = getGraphState(graph);
            GraphDiff diff = diffGraph(graphState1, graphState2);
            diff.printDebug(System.out);
            /*
             * null: 5
             * relationship: 4
             * DocumentaryUnit: 4
             * documentDescription: 4
             * systemEvent: 1
             * datePeriod: 4
             */
            assertEquals(count + 22, getNodeCount(graph));
            DocumentaryUnit unit = manager.getEntity("nl-r1-kd3", DocumentaryUnit.class);
            assertEquals(ps, unit.getRepository());
        }
    }
}
