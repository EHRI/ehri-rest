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

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class Wp2YvEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2YvEadTest.class);
    protected final String SINGLE_EAD = "wp2_yv_ead.xml";
    protected final String C1 = "O.64.2-A.";
    protected final String C2 = "O.64.2-A.A.";
    protected final String C3 = "3685529";
    protected final String C3_ID = "nl-r1-o_64_2-a-a-3685529";
    protected final String FONDS = "O.64.2";

    @Test
    public void testImportItemsT() throws Exception {

        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords")
                .withDataValue(Ontology.NAME_KEY, "WP2 Keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.288");
        Vocabulary vocabulary = new CrudViews<>(graph, Vocabulary.class)
                .create(vocabularyBundle, validUser);
        logger.debug(vocabulary.getId());
        Concept concept_288 = new CrudViews<>(graph, Concept.class).create(conceptBundle, validUser);
        vocabulary.addItem(concept_288);

        Vocabulary vocabularyTest = manager.getEntity("wp2_keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);

        final String logMessage = "Importing Yad Vashem EAD";

        int count = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class)
                .withProperties("wp2ead.properties");
        ImportLog log = importManager.importFile(ios, logMessage);

        //printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 4 more maintenance events
        // - 4 more DocumentaryUnits fonds C1 C2 C3 
        // - 4 more DocumentDescription
        // - 1 more DatePeriod 0 0 1
        // - 11 UndeterminedRelationship, 0 0 0 11
        // - 5 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 Link as resolved relationship 

        int newCount = count + 31;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        DocumentaryUnit c3 = graph.frame(getVertexByIdentifier(graph, C3), DocumentaryUnit.class);

        assertEquals(fonds, c1.getParent());
        assertEquals(c1, c2.getParent());
        assertEquals(c2, c3.getParent());
        // Expect no duplication of ID parts
        assertEquals(C3_ID, c3.getId());

        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(4, log.getCreated());
        SystemEvent ev = actionManager.getLatestGlobalEvent();
        assertEquals(logMessage, ev.getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(!toList(c3.getLinks()).isEmpty());
        for (Link a : c3.getLinks()) {
            logger.debug(a.getLinkType() + " " + a.getDescription());
            assertEquals("subjectAccess", a.getLinkType());
            assertEquals(1, Iterables.size(a.getLinkBodies()));
        }

        List<Accessible> subjects = toList(ev.getSubjects());
        int countSubject = 0;
        for (Accessible subject : subjects) {
            logger.info("identifier: " + subject.getId());
            countSubject++;
        }
        assertTrue(countSubject > 0);

        assertEquals(4, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(repository, fonds.getPermissionScope());
        assertEquals(fonds, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c3.getPermissionScope());

        // Check the author of the top level description
        for (DocumentaryUnitDescription d : fonds.getDocumentDescriptions()) {
            assertEquals("BT", d.getProperty("processInfo"));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager
                .allowUpdates(true)
                .importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(4, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
