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

import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class VC_ARAImporterTest extends AbstractImporterTest {

    protected final String SINGLE_EAD = "araEad.xml";
    protected final String VC_EAD = "araVcEad.xml";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        System.out.println(origCount);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        saxImportManager(EadImporter.class, EadHandler.class, "ara.properties")
                .importInputStream(ios, logMessage);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        printGraph(graph);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


        printGraph(graph);

        InputStream ios_vc = ClassLoader.getSystemResourceAsStream(VC_EAD);
        saxImportManager(VirtualEadImporter.class, VirtualEadHandler.class)
                .withProperties("vc_ara.properties")
                .importInputStream(ios_vc, logMessage);
    }
}
