/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class IcaAtomEadSingleEadTest extends AbstractImporterTest {
    protected final String SINGLE_EAD = "single-ead.xml";

    // Depends on single-ead.xml
    protected final String IMPORTED_ITEM_ID = "C00001";

    @Test
    public void testImportItems() throws Exception {
        Repository agent = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by IcaAtomEadSingleEad";

        int origCount = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class)
                .importInputStream(ios, logMessage);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnit
        // - 1 more DocumentDescription
        // - 2 more DatePeriod
        //TODO: test these UR's
        // - 5 more UndeterminedRelationships
        //TODO: test this UP
        // - 1 more UnknownProperty
        // - 2 more import Event links
        // - 1 more import Event

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);
        assertEquals(Lists.newArrayList(IMPORTED_ITEM_ID + "-alt"),
                unit.<List<String>>getProperty(Ontology.OTHER_IDENTIFIERS));
        for (Description d : unit.getDocumentDescriptions())
            assertEquals("Test EAD Item", d.getName());

        // Test scope and content has correctly decoded data.
        // This tests two bugs:
        //  Space stripping: https://github.com/mikesname/ehri-rest/issues/12
        //  Paragraph ordering: https://github.com/mikesname/ehri-rest/issues/13
        Description firstDesc = unit.getDocumentDescriptions().iterator().next();
        String scopeContent = firstDesc.getProperty("scopeAndContent");
        String expected =
                "This is some test scope and content.\n\n" +
                        "This contains Something & Something else.\n\n" +
                        "This is another paragraph.";

        assertEquals(expected, scopeContent);

        // Check the right nodes get created.
        int createCount = origCount + 13;

        // - 4 more UnderterminedRelationship nodes

        assertEquals(createCount, getNodeCount(graph));

        // Yet we've only created 1 *logical* item...
        assertEquals(1, log.getChanged());

        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Now re-import the same file
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log2 = saxImportManager(EadImporter.class, EadHandler.class)
                .importInputStream(ios2, logMessage);

        // We should no new nodes (not even a SystemEvent)
        assertEquals(createCount, getNodeCount(graph));
        // And no logical item should've been updated
        assertEquals(0, log2.getUpdated());

        // Check permission scopes
        for (Accessible e : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(agent, e.getPermissionScope());
        }
    }
}
