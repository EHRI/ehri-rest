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

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.MaintenanceEventType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the import of a Cegesoma AA EAD file.
 * This file was based on BundesarchiveTest.java.
 */
public class CegesomaAATest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(CegesomaAATest.class);
    protected final String XMLFILE = "CegesomaAA.pxml";
    protected final String ARCHDESC = "AA 1134",
            C01 = "1234",
            C02_01 = "AA 1134 / 32",
            C02_02 = "AA 1134 / 34";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;

    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {

        final String logMessage = "Importing an example Cegesoma EAD";

        int origCount = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        saxImportManager(EadImporter.class, EadHandler.class, "cegesomaAA.properties")
                .importInputStream(ios, logMessage);

        // How many new nodes will have been created? We should have
        /*
         * event links: 6
         * relationship: 34
         * DocumentaryUnit: 5
         * documentDescription: 5
         * systemEvent: 1
         * datePeriod: 4
         * maintenanceEvent: 5
         */
        int newCount = origCount + 60;
        assertEquals(newCount, getNodeCount(graph));

        archdesc = graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);
        c1 = graph.frame(
                getVertexByIdentifier(graph, C01),
                DocumentaryUnit.class);
        c2_1 = graph.frame(
                getVertexByIdentifier(graph, C02_01),
                DocumentaryUnit.class);
        c2_2 = graph.frame(
                getVertexByIdentifier(graph, C02_02),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-aa_1134-1234", c1.getId());
        assertEquals(c1.getId() + "-aa_1134_32", c2_1.getId());
        assertEquals(c1.getId() + "-aa_1134_34", c2_2.getId());

        // There should be one DocumentDescription for the <archdesc>
        for (DocumentaryUnitDescription dd : archdesc.getDocumentDescriptions()) {
            assertEquals("Deelarchief betreffende het actienetwerk Nola (1942-1944)", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
            assertEquals("In het Frans", dd.getProperty("languageOfMaterial"));
            assertEquals("Zie ook AA 1297", dd.getProperty("relatedMaterial"));
            assertTrue(dd.getProperty("notes").toString().startsWith("Nr 1-2-13: fotokopies Bibliothek"));
            assertEquals("Groupe Nola / door D. Martin (Soma, januari 1984, 12 p.)", dd.getProperty("findingAids"));

            for (MaintenanceEvent me : dd.getMaintenanceEvents()) {
                assertEquals("Automatisch gegenereerd door PALLAS systeem", me.getProperty("source"));
                assertEquals("28/03/2013", me.getProperty("date"));
                assertEquals(MaintenanceEventType.created.toString(), me.getProperty("eventType"));
            }
            assertEquals("SOMA_CEGES_72695#NLD", dd.getProperty("sourceFileId"));
        }

        // There should be one DocumentDescription for the (only) <c01>
        for (DocumentaryUnitDescription dd : c1.getDocumentDescriptions()) {
            assertEquals("Documenten betreffende l'Union nationale de la Résistance", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
            assertEquals("SOMA_CEGES_72695#NLD", dd.getProperty("sourceFileId"));
            assertEquals(1, toList(dd.getMaintenanceEvents()).size());
        }

        // There should be one DocumentDescription for the (second) <c02>
        for (DocumentaryUnitDescription dd : c2_2.getDocumentDescriptions()) {
            assertEquals("Wetteksten (U.) S.R.A.", dd.getName());
            assertEquals("nld", dd.getLanguageOfDescription());
            assertEquals("item", dd.getProperty("levelOfDescription"));
        }

        /*
         * Test hierarchy
         */
        assertEquals(1, archdesc.countChildren());
        for (DocumentaryUnit du : archdesc.getChildren()) {
            assertEquals(C01, du.getIdentifier());
        }
        //test dates
        for (DocumentaryUnitDescription d : c2_1.getDocumentDescriptions()) {
            // Single date is just a string
            assertFalse(d.getPropertyKeys().contains("unitDates"));
            for (DatePeriod dp : d.getDatePeriods()) {
                assertEquals("1944-01-01", dp.getStartDate());
                assertEquals("1948-12-31", dp.getEndDate());
            }
        }

        // Fonds has two dates with different types -> list
        for (DocumentaryUnitDescription d : archdesc.getDocumentDescriptions()) {
            // start and end dates correctly parsed and setup

            assertFalse(d.getPropertyKeys().contains("unitDates"));
            List<DatePeriod> dps = toList(d.getDatePeriods());
            assertEquals(2, dps.size());
            for (DatePeriod dp : dps) {
                String dateDesc = dp.getProperty(Ontology.DATE_HAS_DESCRIPTION);
                logger.info("Date object: {}", dateDesc);
                if (dateDesc.equals("1944-1948")) {
                    assertEquals("1944-01-01", dp.getStartDate());
                    assertEquals("1948-12-31", dp.getEndDate());
                } else if (dateDesc.equals("1944-1979")) {
                    assertEquals("1979-12-31", dp.getEndDate());
                }
            }
        }
    }
}
