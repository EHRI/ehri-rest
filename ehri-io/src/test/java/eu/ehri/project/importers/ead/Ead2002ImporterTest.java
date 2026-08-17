/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.ead;

import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class Ead2002ImporterTest extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD 2002";

        int origCount = getNodeCount(graph);
        List<VertexProxy> before = getGraphState(graph);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream("simple-ead2002.xml")) {
            saxImportManager(EadImporter.class, EadHandler.class, "ead2002.properties")
                    .importInputStream(ios, logMessage);
        }
        List<VertexProxy> after = getGraphState(graph);
        diffGraph(before, after).printDebug(System.out, true);

        /*
         * Nodes created:
         *  - 1 unit
         *  - 1 description
         *  - 1 system event
         *  - 2 event links
         *  - 1 date period
         *  - 1 unknown properties
         */
        assertEquals(origCount + 7, getNodeCount(graph));

        DocumentaryUnitDescription desc = manager.getEntity("nl-r1-t1.eng-test_1_eng", DocumentaryUnitDescription.class);
        assertNotNull(desc);
        assertThat(desc.getProperty("languageOfMaterial"), containsInAnyOrder("eng"));
        assertThat(desc.getProperty("scriptOfMaterial"), containsInAnyOrder("Latn"));
    }
}
