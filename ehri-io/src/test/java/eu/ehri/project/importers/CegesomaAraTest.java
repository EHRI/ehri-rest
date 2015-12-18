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

package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the import of a Cegesoma AA EAD file. This file was based on BundesarchiveTest.java.
 */
public class CegesomaAraTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(CegesomaAraTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE = "CegesomaAA.pxml";
    protected final String ARA_XMLFILE = "araEad.xml";
    protected final String ARCHDESC = "AA 1134",
            C01 = "1234",
            C02_01 = "AA 1134 / 32",
            C02_02 = "AA 1134 / 34";
    DocumentaryUnit archdesc, c1, c2_1, c2_2;
    int origCount = 0;

    @Test
    public void cegesomaTest() throws ItemNotFound, IOException, ValidationError, InputParseError {

        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example Cegesoma EAD";

        origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("cegesomaAA.properties")).importFile(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
//       diff.printDebug(System.out);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        /**
         * event links: 6 relationship: 34 documentaryUnit: 5 documentDescription: 5 systemEvent: 1 datePeriod: 4
         * maintenanceEvent: 1
         */
        int newCount = origCount + 56;
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

        for (String key : archdesc.getPropertyKeys()) {
            logger.debug(key + " " + archdesc.getProperty(key));
        }
        assertTrue(((List<String>) archdesc.getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AA 627"));

        InputStream ios_ara = ClassLoader.getSystemResourceAsStream(ARA_XMLFILE);
        importManager = new SaxImportManager(graph, repository, validUser, AraEadImporter.class, EadHandler.class, new XmlImportProperties("ara.properties"))
                .setTolerant(Boolean.TRUE);

        ImportLog log_ara = importManager.importFile(ios_ara, logMessage);
        for (String key : archdesc.getPropertyKeys()) {
            logger.debug(key + " " + archdesc.getProperty(key));
        }
        assertTrue(archdesc.getPropertyKeys().contains(Ontology.OTHER_IDENTIFIERS));
        assertTrue(((List<String>) archdesc.getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AA 627"));
        assertTrue(((List<String>) archdesc.getProperty(Ontology.OTHER_IDENTIFIERS)).contains("AC559"));
        

    }
}
