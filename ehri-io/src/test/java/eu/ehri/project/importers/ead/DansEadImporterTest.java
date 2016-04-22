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
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class DansEadImporterTest extends AbstractImporterTest {

    protected final String SINGLE_EAD = "dans_convertedead_part.xml";

    // Depends on fixtures
    protected final String ARCHDESC = "easy-collection:2",
            C1 = "urn:nbn:nl:ui:13-4i8-gpf",
            C2 = "urn:nbn:nl:ui:13-qa8-3r5";


    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        System.out.println(origCount);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        saxImportManager(EadImporter.class, EadHandler.class, "dansead.properties")
                .importFile(ios, logMessage);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        printGraph(graph);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        printGraph(graph);
        /*
         * we should have
         * 
         * null: 5
         * relationship: 6
         * DocumentaryUnit: 4
         * documentDescription: 4
         * maintenanceEvent: 4
         * systemEvent: 1
         * datePeriod: 5  //there are 6 unitdates in the xml, however two are identical and get merged into 1
         */
        int newCount = origCount + 29;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        Iterator<DocumentaryUnitDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while (i.hasNext()) {
            DocumentaryUnitDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("nld", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);
    }
}
