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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the import of a Cegesoma CA EAD file.
 * This file was based on BundesarchiveTest.java.
 */
public class CegesomaCATest extends AbstractImporterTest {

    protected final String XMLFILE_NL = "CS-foto-188845-nl.xml";
    protected final String ARCHDESC = "CA FE 1437";

    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {
        final String logMessage = "Importing an example Cegesoma EAD";
        int origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE_NL);
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, "cegesomaCA.properties")
                .importInputStream(ios, logMessage);
        assertTrue(log.hasDoneWork());
        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits (archdesc)
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 2 more subjectAccess nodes
        // - 1 UP
        // - 2 more import Event links (1 for each Unit, 1 for the User)
        // - 1 more import Event
        // MaintenanceEvent 1
        int newCount = origCount + 10;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-ca_fe_1437", archdesc.getId());

        /**
         * Test titles
         */
        // There should be one DocumentDescription for the <archdesc>
        for (DocumentaryUnitDescription dd : archdesc.getDocumentDescriptions()) {
            assertTrue(dd.getName().startsWith("Dessin caricatural d'un juif ayant "));
            assertEquals("fra", dd.getLanguageOfDescription());
        }


        // Fonds has two dates with different types -> list
        for (DocumentaryUnitDescription d : archdesc.getDocumentDescriptions()) {
            // unitDates still around?
            assertFalse(d.getPropertyKeys().contains("unitDates"));

            // start and end dates correctly parsed and setup
            List<DatePeriod> dp = toList(d.getDatePeriods());
            assertEquals(1, dp.size());
            assertEquals("1940-01-01", dp.get(0).getStartDate());
            assertEquals("1945-12-31", dp.get(0).getEndDate());
        }
    }
}
