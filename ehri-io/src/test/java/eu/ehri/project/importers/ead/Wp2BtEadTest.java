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

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class Wp2BtEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2BtEadTest.class);
    protected final String SINGLE_EAD = "wp2_bt_ead_small.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String C1_A = "000.001.0";
    protected final String C1_B = "000.002.0";
    protected final String C1_A_C2 = "000.001.1";
    protected final String C1_B_C2_A = "000.002.1";
    protected final String C1_B_C2_B = "000.002.2";
    protected final String FONDS = "wp2-bt";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getEntity(TEST_REPO, Repository.class);
        Bundle vocabularyBundle = Bundle.of(EntityClass.CVOC_VOCABULARY)
                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords")
                .withDataValue(Ontology.NAME_KEY, "WP2 Keywords");
        Bundle conceptBundle = Bundle.of(EntityClass.CVOC_CONCEPT)
                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.716");
        Vocabulary vocabulary = api(validUser).create(vocabularyBundle, Vocabulary.class);
        logger.debug(vocabulary.getId());
        Concept concept_716 = api(validUser).create(conceptBundle, Concept.class);
        vocabulary.addItem(concept_716);


        Vocabulary vocabularyTest = manager.getEntity("wp2_keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);

        final String logMessage = "Importing Beit Terezin EAD";
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        int count = getNodeCount(graph);
        InputStream iosVC = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = saxImportManager(
                EadImporter.class, EadHandler.class, "wp2ead.properties");
        ImportLog logVC = importManager.importInputStream(iosVC, logMessage);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        // How many new nodes will have been created? We should have
        // - 6 more DocumentaryUnits fonds 2C1 3C2
        // - 6 more DocumentDescription
        // - 6 more maintenance events
        // - 1 more DatePeriod 0 0 1 
        // - 17 UndeterminedRelationship, 0 2 2 4 4 5
        // - 7 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 Annotation as resolved relationship 
        int newCount = count + 45;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);
        List<DocumentaryUnitDescription> descriptions = Lists.newArrayList(fonds.getDocumentDescriptions());
        assertEquals(1, descriptions.size());
        assertEquals("mul", descriptions.get(0).getLanguageOfDescription());

        // check the child items
        DocumentaryUnit c1_a = graph.frame(getVertexByIdentifier(graph, C1_A), DocumentaryUnit.class);
        DocumentaryUnit c1_b = graph.frame(getVertexByIdentifier(graph, C1_B), DocumentaryUnit.class);
        DocumentaryUnit c1_a_c2 = graph.frame(getVertexByIdentifier(graph, C1_A_C2), DocumentaryUnit.class);
        DocumentaryUnit c1_b_c2_a = graph.frame(getVertexByIdentifier(graph, C1_B_C2_A), DocumentaryUnit.class);
        DocumentaryUnit c1_b_c2_b = graph.frame(getVertexByIdentifier(graph, C1_B_C2_B), DocumentaryUnit.class);

        assertEquals(fonds, c1_a.getParent());
        assertEquals(fonds, c1_b.getParent());

        assertEquals(c1_a, c1_a_c2.getParent());

        assertEquals(c1_b, c1_b_c2_a.getParent());
        assertEquals(c1_b, c1_b_c2_b.getParent());

        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(6, logVC.getCreated());
        SystemEvent ev = actionManager.getLatestGlobalEvent();
        assertEquals(logMessage, ev.getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(!toList(c1_b.getLinks()).isEmpty());
        for (Link a : c1_b.getLinks()) {
            logger.debug(a.getLinkType());
        }

        List<Accessible> subjects = toList(ev.getSubjects());
        for (Accessible subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        assertEquals(6, subjects.size());
        assertEquals(logVC.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
        assertEquals(fonds, c1_a.getPermissionScope());
        assertEquals(fonds, c1_b.getPermissionScope());
        assertEquals(c1_a, c1_a_c2.getPermissionScope());
        assertEquals(c1_b, c1_b_c2_a.getPermissionScope());
        assertEquals(c1_b, c1_b_c2_b.getPermissionScope());

        // Check the author of the description
        for (DocumentaryUnitDescription d : fonds.getDocumentDescriptions()) {
            assertEquals("EHRI", d.getProperty("processInfo"));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager
                .allowUpdates(true)
                .importInputStream(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(6, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
