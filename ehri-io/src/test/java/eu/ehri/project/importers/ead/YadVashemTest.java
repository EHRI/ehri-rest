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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.ead;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.test.IOHelpers;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class YadVashemTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(YadVashemTest.class);
    protected final String SINGLE_EAD = "YV_m19_eng.xml";
    protected final String SINGLE_EAD_HEB = "YV_m19_heb.xml";
    protected final String SINGLE_EAD_C1 = "YV_c1.xml";
    // Depends on fixtures
    protected final String ARCHDESC = "M.19",
            C1 = "M.19/7",
            C2 = "M.19/7.1";

    /**
     * The test repository contains a unit (loaded from the fixtures) that matches
     * the unit described in the test file that is imported here.
     * This test checks that the existing description (which has a different source)
     * is untouched and the description from the test file is added.
     * NB: this behaviour relies on the 'merging' option being enabled on the
     * importer.
     */
    @Test
    public void testWithExistingDescription() throws Exception {
        final String logMessage = "Importing a single EAD";
        DocumentaryUnit m19 = manager.getEntity("nl-r1-m19", DocumentaryUnit.class);

        assertEquals("m19", m19.getIdentifier());
        assertEquals(1, toList(m19.getDocumentDescriptions()).size());

        int count = getNodeCount(graph);
        System.out.println(count);
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_C1);
        ImportOptions options = ImportOptions.properties("yadvashem.properties")
                .withUpdates(true)
                .withUseSourceId(true);
        saxImportManager(EadImporter.class, EadHandler.class, options)
                .importInputStream(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        /*
         * null: 4
         * relationship: 4
         * DocumentaryUnit: 2
         * documentDescription: 3
         * maintenance event: 3
         * systemEvent: 1
         * datePeriod: 1
         */

        assertEquals(count + 18, getNodeCount(graph));
        assertEquals(2, toList(m19.getDocumentDescriptions()).size());
        for (DocumentaryUnitDescription desc : m19.getDocumentDescriptions()) {
            logger.debug("Document description graph ID: {}", desc.getId());
            assertTrue(desc.getId().equals("nl-r1-m19.eng") || desc.getId().equals("nl-r1-m19.eng-c1_eng"));
        }
    }

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        DocumentaryUnit m19 = manager.getEntity("nl-r1-m19", DocumentaryUnit.class);

        assertEquals("m19", m19.getIdentifier());
        assertEquals(1, toList(m19.getDocumentDescriptions()).size());
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportOptions options = ImportOptions.properties("yadvashem.properties")
                .withUpdates(true)
                .withUseSourceId(true);
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class, options);
        importManager.importInputStream(ios, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
       /*
        * null: 4
        * relationship: 5 (2 creator, 1 place, 1 subject, 1 geog)
        * DocumentaryUnit: 2
        * documentDescription: 3
        * maintenance event: 3
        * property: 1
        * systemEvent: 1
        * datePeriod: 1
        */
        assertEquals(count + 20, getNodeCount(graph));
        //ENG also imported:
        assertEquals(2, toList(m19.getDocumentDescriptions()).size());
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        Iterator<DocumentaryUnitDescription> i = c1.getDocumentDescriptions().iterator();
        int nrOfDesc = 0;
        while (i.hasNext()) {
            DocumentaryUnitDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            assertEquals("eng", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(1, nrOfDesc);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_HEB);
        importManager.withUpdates(true).importInputStream(ios, logMessage);

        //HEB also imported:
        assertEquals(3, toList(m19.getDocumentDescriptions()).size());
        logger.debug("size: " + toList(m19.getDocumentDescriptions()).size());
        for (DocumentaryUnitDescription m19desc : m19.getDocumentDescriptions()) {
            logger.debug(m19desc.getId() + ":" + m19desc.getLanguageOfDescription() + ":" + m19desc.getProperty(Ontology.SOURCEFILE_KEY));
        }

        i = c1.getDocumentDescriptions().iterator();
        nrOfDesc = 0;
        while (i.hasNext()) {
            DocumentaryUnitDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            //assertEquals("fin", desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(2, nrOfDesc);

        i = c2.getDocumentDescriptions().iterator();
        nrOfDesc = 0;
        while (i.hasNext()) {
            DocumentaryUnitDescription desc = i.next();
            System.out.println("language = " + desc.getLanguageOfDescription());
            nrOfDesc++;
        }
        assertEquals(2, nrOfDesc);
        int count_heb = getNodeCount(graph);

        System.out.println(count + " " + count + " " + count_heb);
        printGraph(graph);

        ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD_HEB);
        // Before...
        List<VertexProxy> graphState1_heb = getGraphState(graph);
        logger.debug("reimport HEB");
        importManager.importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2_heb = getGraphState(graph);
        GraphDiff diff_heb = diffGraph(graphState1_heb, graphState2_heb);
        diff_heb.printDebug(System.out);
        logger.debug("reimport HEB");
        //HEB re imported:
        assertEquals(3, toList(m19.getDocumentDescriptions()).size());
        assertEquals(count_heb, getNodeCount(graph));
    }

    @Test
    public void testIdempotentImportViaXmlAndZip() throws Exception {
        String resource = "MS1_O84_HEB-partial-unicode.xml";
        InputStream ios = ClassLoader.getSystemResourceAsStream(resource);
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class, "yadvashem.properties");
        ImportLog log = importManager.importInputStream(ios, "Test");
        assertEquals(1, log.getCreated());

        File temp = File.createTempFile("test-zip", ".zip");
        temp.deleteOnExit();
        IOHelpers.createZipFromResources(temp, resource);

        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(temp.toPath()));
             ArchiveInputStream archiveInputStream = new ArchiveStreamFactory(
                     StandardCharsets.UTF_8.displayName()).createArchiveInputStream(bis)) {
            ImportLog log2 = importManager
                    .withUpdates(true)
                    .importArchive(archiveInputStream, "Test 2");
            assertEquals(1, log2.getUnchanged());
            assertEquals(0, log2.getUpdated());
        }
    }
}
