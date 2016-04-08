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
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class IcaAtomEadImporterTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(IcaAtomEadImporterTest.class);
    protected final String SINGLE_EAD = "hierarchical-ead.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String FONDS_LEVEL = "Ctop level fonds";
    protected final String SUBFONDS_LEVEL = "C00001";
    protected final String C2 = "C00002";
    protected final String C2_1 = "C00002-1";
    protected final String C2_2 = "C00002-2";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportManager importManager = new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class, IcaAtomEadHandler.class)
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);
//        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 5 more DocumentaryUnits
        // - 5 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more UnknownProperties
        // - 1 UndeterminedRelationship, from origination/name
        // - 6 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = count + 20;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY,
                FONDS_LEVEL);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds_unit = graph.frame(
                getVertexByIdentifier(graph, FONDS_LEVEL),
                DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(
                getVertexByIdentifier(graph, SUBFONDS_LEVEL),
                DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(
                getVertexByIdentifier(graph, C2),
                DocumentaryUnit.class);
        DocumentaryUnit c2_1 = graph.frame(
                getVertexByIdentifier(graph, C2_1),
                DocumentaryUnit.class);
        DocumentaryUnit c2_2 = graph.frame(
                getVertexByIdentifier(graph, C2_2),
                DocumentaryUnit.class);

        // Ensure that the first child's parent is unit
        assertEquals(c1, c2.getParent());

        // Ensure the grandkids parents is c1
        assertEquals(c2, c2_1.getParent());
        assertEquals(c2, c2_2.getParent());

        // Ensure unit the the grandparent of cc1
        List<DocumentaryUnit> ancestors = toList(c2_1.getAncestors());
        assertEquals(fonds_unit, ancestors.get(ancestors.size() - 1));

        // Ensure the import action has the right number of subjects.
//        Iterable<Action> actions = unit.getHistory();
        // Check we've created 4 items
        assertEquals(5, log.getCreated());
        SystemEvent ev = actionManager.getLatestGlobalEvent();
        assertEquals(logMessage, ev.getLogMessage());


        List<Accessible> subjects = toList(ev.getSubjects());
        for (Accessible subject : subjects)
            logger.info("identifier: " + subject.getId());

        assertEquals(5, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check all descriptions have an IMPORT creationProcess
        for (Description d : c1.getDocumentDescriptions()) {
            assertEquals(Description.CreationProcess.IMPORT, d.getCreationProcess());
        }

        // Check permission scopes
        assertEquals(agent, fonds_unit.getPermissionScope());
        assertEquals(fonds_unit, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c2_1.getPermissionScope());
        assertEquals(c2, c2_2.getPermissionScope());

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(
                ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(5, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
