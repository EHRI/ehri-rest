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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.ead;

import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
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

/**
 * Test the import of a Ifz EAD file.
 * This file was based on BundesarchiveTest.java.
 */
public class IfzTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "ifz_ED.xml";
    protected final String ARCHDESC = "G",
            C01 = "G_1",
            C02 = "G_2",
            C03_01 = "G 35 / 1",
            C03_02 = "MB 35 / 10";
    DocumentaryUnit archdesc, c1, c2, c3_1, c3_2;
    int origCount = 0;

    @Test
    public void ifzTest() throws ItemNotFound, IOException, ValidationError, InputParseError {

        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Ifz EAD";

        origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("ifz.properties")).importFile(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        /**
         * null: 6
         * relationship: 11
         * DocumentaryUnit: 5
         * documentDescription: 5
         * maintenanceEvent: 30
         * systemEvent: 1
         * 1 date
         */
        int newCount = origCount + 59;
        assertEquals(newCount, getNodeCount(graph));

        archdesc = graph.frame(
                getVertexByIdentifier(graph, ARCHDESC),
                DocumentaryUnit.class);
        c1 = graph.frame(
                getVertexByIdentifier(graph, C01),
                DocumentaryUnit.class);
        c2 = graph.frame(
                getVertexByIdentifier(graph, C02),
                DocumentaryUnit.class);
        c3_2 = graph.frame(
                getVertexByIdentifier(graph, C03_02),
                DocumentaryUnit.class);

        // Test ID generation is correct
        assertEquals("nl-r1-g-1", c1.getId());
        assertEquals(c1.getId() + "-g_2", c2.getId());
        assertEquals(c2.getId() + "-mb_35_10", c3_2.getId());

        /**
         * Test titles
         */
        // There should be one DocumentDescription for the <archdesc>
        for (DocumentaryUnitDescription dd : archdesc.getDocumentDescriptions()) {
            assertEquals("TO BE FILLED", dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            List<String> s = Lists.newArrayList("TO BE FILLED with selection", "IfZ");
            assertEquals(s, dd.asVertex().<List<String>>getProperty("processInfo"));
            assertEquals("recordgrp", dd.getProperty("levelOfDescription"));

        }

        // There should be one DocumentDescription for the (only) <c01>
        for (DocumentaryUnitDescription dd : c1.getDocumentDescriptions()) {
            assertEquals("Internationale u. ausländische Gerichtsorte", dd.getName());
            assertEquals("deu", dd.getLanguageOfDescription());
            assertEquals("series", dd.getProperty("levelOfDescription"));
        }

        /**
         * Test hierarchy
         */
        assertEquals(1L, archdesc.getChildCount());
        for (DocumentaryUnit du : archdesc.getChildren()) {
            assertEquals(C01, du.getIdentifier());
        }
        //test dates
        for (DocumentaryUnitDescription d : c3_2.getDocumentDescriptions()) {
            // Single date is just a string 1945/1957
            assertFalse(d.getPropertyKeys().contains("unitDates"));
            for (DatePeriod dp : d.getDatePeriods()) {
                assertEquals("1945-01-01", dp.getStartDate());
                assertEquals("1957-12-31", dp.getEndDate());
            }
        }

        // Fonds has two dates with different types -> list
        for (DocumentaryUnitDescription d : archdesc.getDocumentDescriptions()) {
            // start and end dates correctly parsed and setup
            List<DatePeriod> dp = toList(d.getDatePeriods());
            assertEquals(0, dp.size());
        }

    }
}
