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

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class JmpEadTest extends AbstractImporterTest {

    protected final String SINGLE_EAD = "JMP20141117.xml";

    protected final String FONDS = "COLLECTION.JMP.ARCHIVE/NAD3";

    @Test
    public void testImportItems() throws Exception {

        Repository agent = manager.getEntity(TEST_REPO, Repository.class);

        final String logMessage = "Importing JMP EAD";

        int count = getNodeCount(graph);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD)) {
            ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, "wp2ead.properties")
                    .importInputStream(ios, logMessage);

            // How many new nodes will have been created? We should have
            // - 1 more DocumentaryUnits fonds C1 C2 C3 4,5,6
            // - 1 more DocumentDescription
            // - 0 more DatePeriod 0 0 1
            // - 3 UndeterminedRelationship, 0 0 0 11
            // - 2 more import Event links (4 for every Unit, 1 for the User)
            // - 1 more import Event
            // - 0 Annotation as resolved relationship
            // - 1 unknownProperty

            printGraph(graph);
            int newCount = count + 12;
            assertEquals(newCount, getNodeCount(graph));

            Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
            assertTrue(docs.iterator().hasNext());
            DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

            for (DocumentaryUnitDescription d : fonds.getDocumentDescriptions()) {
                List<String> langs = d.getProperty("languageOfMaterial");
                assertFalse(langs.isEmpty());
                assertEquals("deu", langs.get(0));
            }

            List<Accessible> subjects = toList(actionManager.getLatestGlobalEvent().getSubjects());

            //huh, should be 7?
            assertEquals(1, subjects.size());
            assertEquals(log.getChanged(), subjects.size());

            // Check permission scopes
            assertEquals(agent, fonds.getPermissionScope());
        }
    }
}
