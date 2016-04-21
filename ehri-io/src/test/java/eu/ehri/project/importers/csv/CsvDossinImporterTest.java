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

package eu.ehri.project.importers.csv;

import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.PermissionScope;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class CsvDossinImporterTest extends AbstractImporterTest {

    protected final String SINGLE_EAD = "dossin.csv";
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItemsT() throws Exception {

        PermissionScope ps = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing some Dossin records";

        int count = getNodeCount(graph);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        new CsvImportManager(graph, ps, validUser, false, false, EadImporter.class).importFile(ios, logMessage);
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
//        printGraph(graph);
        assertEquals(count + 22, getNodeCount(graph));

        DocumentaryUnit unit = graph.frame(
                getVertexByIdentifier(graph, "kd3"),
                DocumentaryUnit.class);

        assertNotNull(unit);
        Repository r = unit.getRepository();
        System.out.println(r.getId());
        Repository repo = graph.frame(
                getVertexByIdentifier(graph, TEST_REPO),
                Repository.class);
        assertEquals(repo, r);

    }
}
