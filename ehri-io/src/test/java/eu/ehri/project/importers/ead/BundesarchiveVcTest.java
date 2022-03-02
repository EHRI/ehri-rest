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

package eu.ehri.project.importers.ead;

import com.google.common.collect.Iterables;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class BundesarchiveVcTest extends AbstractImporterTest {

    protected final String XMLFILE = "BA_split.xml";
    protected final String VCFILE = "BA_vc.xml";
    protected final String ARCHDESC = "NS 1";

    @Test
    public void bundesarchiveTest() throws Exception {

        final String logMessage = "Importing a part of the Split Bundesarchive EAD";

        int origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        saxImportManager(EadImporter.class, EadHandler.class, "bundesarchive.properties")
                .importInputStream(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more UnknownProperties
        // - 3 more Relationships
        // - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 5 more MaintenanceEvents (4 revised, 1 created)
        int newCount = origCount + 15;

        assertEquals(newCount, getNodeCount(graph));

        graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);

        InputStream iosvc = ClassLoader.getSystemResourceAsStream(VCFILE);
        saxImportManager(VirtualEadImporter.class, VirtualEadHandler.class, "vc.properties")
                .importInputStream(iosvc, logMessage);
        printGraph(graph);

        VirtualUnit ss = graph.frame(getVertexByIdentifier(graph, "0.0.0.0"), VirtualUnit.class);
        assertEquals(1, Iterables.size(ss.getIncludedUnits()));
    }
}
