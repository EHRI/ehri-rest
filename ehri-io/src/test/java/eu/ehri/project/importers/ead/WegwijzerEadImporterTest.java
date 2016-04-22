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

package eu.ehri.project.importers.ead;

import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class WegwijzerEadImporterTest extends AbstractImporterTest {

    protected final String SINGLE_EAD = "wegwijzer.xml";

    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        System.out.println(origCount);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = saxImportManager(
                EadImporter.class, EadHandler.class, "wegwijzer.properties");

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        importManager.importFile(ios, logMessage);
        printGraph(graph);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        /**
         * null: 7
         * relationship: 27
         * DocumentaryUnit: 6
         * documentDescription: 6
         * maintenanceEvent: 6
         * systemEvent: 1
         * datePeriod: 6
         */
        assertEquals(origCount + 59, getNodeCount(graph));
    }
}
