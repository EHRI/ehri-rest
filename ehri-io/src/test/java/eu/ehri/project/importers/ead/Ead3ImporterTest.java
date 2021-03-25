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

package eu.ehri.project.importers.ead;

import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class Ead3ImporterTest extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD 3";

        int origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream("simple-ead3.xml");
        List<VertexProxy> before = getGraphState(graph);
        saxImportManager(EadImporter.class, EadHandler.class, "ead3.properties")
                .importInputStream(ios, logMessage);
        List<VertexProxy> after = getGraphState(graph);
        diffGraph(before, after).printDebug(System.out, true);

        // TODO: lots of information is missing here!
        /*
         * Nodes created:
         *  - 2 unit
         *  - 2 description
         *  - 1 system event
         *  - 3 event link
         *  - 5 access points
         *  - 2 maintenance events
         *  - 5 date periods
         *  - 1 unknown properties
         */
        assertEquals(origCount + 21, getNodeCount(graph));
    }
}
