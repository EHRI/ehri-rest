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
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class Ead3ImporterTest extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD 3";

        int origCount = getNodeCount(graph);
        List<VertexProxy> before = getGraphState(graph);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream("simple-ead3.xml")) {
            saxImportManager(EadImporter.class, EadHandler.class, "ead3.properties")
                    .importInputStream(ios, logMessage);
        }
        List<VertexProxy> after = getGraphState(graph);
        diffGraph(before, after).printDebug(System.out, true);

        // TODO: lots of information is missing here!
        /*
         * Nodes created:
         *  - 2 unit
         *  - 2 description
         *  - 1 system event
         *  - 3 event link
         *  - 4 access points
         *  - 2 maintenance events
         *  - 5 date periods
         *  - 1 unknown properties
         */
        assertEquals(origCount + 20, getNodeCount(graph));

        DocumentaryUnitDescription desc = manager.getEntity("nl-r1-t1.eng-test_1_eng", DocumentaryUnitDescription.class);
        assertNotNull(desc);
        assertThat(desc.getProperty("languageOfMaterial"), containsInAnyOrder("eng"));
        assertThat(desc.getProperty("scriptOfMaterial"), containsInAnyOrder("Latn"));

        // The origination/persname's two <part>s ("EHRI", "2010-2024") are joined
        // into a single access point's name, rather than becoming two access points.
        List<AccessPoint> creators = toList(desc.getAccessPoints());
        assertEquals(1, creators.stream().filter(ap -> "EHRI 2010-2024".equals(ap.getName())).count());
    }
}
