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
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.base.Accessible;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class Jmp130Test extends AbstractImporterTest {

    protected final String SINGLE_EAD = "JMP-130.xml";

    // Depends on hierarchical-ead.xml
    protected final String FONDS = "COLLECTION.JMP.ARCHIVE/130";

    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing JMP EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class)
                .withProperties("jmp.properties")
                .importInputStream(ios, logMessage);

        List<VertexProxy> graphState1 = getGraphState(graph);
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
            assertFalse(langs.isEmpty());
            assertEquals("ces", langs.get(0));
        }

        List<Accessible> subjects = toList(actionManager.getLatestGlobalEvent().getSubjects());
        assertEquals(1, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(repository, fonds.getPermissionScope());
    }
}
