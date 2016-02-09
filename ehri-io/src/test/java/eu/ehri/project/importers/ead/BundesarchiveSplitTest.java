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

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class BundesarchiveSplitTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "BA_split.xml";
    protected final String ARCHDESC = "NS 1";

    @Test
    public void bundesarchiveTest() throws ItemNotFound, IOException, ValidationError, InputParseError {

        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of the Split Bundesarchive EAD";

        int origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        new SaxImportManager(graph, agent, validUser,
                EadImporter.class, EadHandler.class,
                new XmlImportProperties("bundesarchive.properties")).importFile(ios, logMessage);

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
        int newCount = origCount + 9 + 1 + 4 + 1;
        printGraph(graph);

        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archUnit = graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);

        // Test ID generation andhierarchy
        assertEquals("nl-r1-ns_1", archUnit.getId());
        assertTrue(archUnit.getPropertyKeys().contains(Ontology.OTHER_IDENTIFIERS));

        assertNull(archUnit.getParent());
        assertEquals(agent, archUnit.getRepository());
        assertEquals(agent, archUnit.getPermissionScope());

        //test titles
        for (DocumentaryUnitDescription d : archUnit.getDocumentDescriptions()) {
            assertEquals("Reichsschatzmeister der NSDAP", d.getName());
        }
        //test dates
        for (DocumentaryUnitDescription d : archUnit.getDocumentDescriptions()) {
            // Single date is just a string
            assertFalse(d.getPropertyKeys().contains("unitDates"));
            List<DatePeriod> datePeriods = Lists.newArrayList(d.getDatePeriods());
            assertEquals(1, datePeriods.size());
            assertEquals("1906-01-01", datePeriods.get(0).getStartDate());
            assertEquals("1919-12-31", datePeriods.get(0).getEndDate());
        }
    }
}
