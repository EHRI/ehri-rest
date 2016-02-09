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

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class Jmp130Test extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Jmp130Test.class);
    protected final String SINGLE_EAD = "JMP-130.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml

    protected final String FONDS = "COLLECTION.JMP.ARCHIVE/130";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getEntity(TEST_REPO, Repository.class);

        final String logMessage = "Importing JMP EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("jmp.properties"));

        importManager.setTolerant(Boolean.TRUE);

        List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = importManager.importFile(ios, logMessage);
        printGraph(graph);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        /**
         * null: 2
         * relationship: 5
         * DocumentaryUnit: 1
         * documentDescription: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 1
         */

        printGraph(graph);
        int newCount = count + 12;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        for (DocumentaryUnitDescription d : fonds.getDocumentDescriptions()) {
            List<String> langs = d.getProperty("languageOfMaterial");
            assertTrue(langs.size() > 0);
            assertEquals("ces", langs.get(0));
        }

        List<Accessible> subjects = toList(actionManager.getLatestGlobalEvent().getSubjects());
        assertEquals(1, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
    }
}
