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

package eu.ehri.project.importers.eac;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.test.GraphTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class EacImporterTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(EacImporterTest.class);

    @Test
    public void testAbwehrWithOUTAllReferredNodes() throws Exception {
        String eacFile = "abwehr.xml";
        String logMessage = String.format("Importing EAC %s without creating any annotation, " +
                "since the targets are not present in the graph", eacFile);
        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(eacFile);

        // Before...
        List<VertexProxy> graphState1 = GraphTestBase.getGraphState(graph);
        saxImportManager(EacImporter.class, EacHandler.class)
                .withScope(SystemScope.getInstance())
                .importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = GraphTestBase.getGraphState(graph);
        GraphDiff diff = GraphTestBase.diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        /*
         * How many new nodes will have been created? We should have
         * null: 2
         * historicalAgent: 1
         * property: 1
         * maintenanceEvent: 2
         * systemEvent: 1
         * historicalAgentDescription: 1
         * datePeriod: 1
         */
        assertEquals(count + 9, getNodeCount(graph));

        HistoricalAgent abwehr = manager.getEntity("381", HistoricalAgent.class);
        assertEquals(Entities.HISTORICAL_AGENT, abwehr.getType());
        assertEquals(0, toList(abwehr.getAnnotations()).size());
        Description desc = abwehr.getDescriptions().iterator().next();
        DatePeriod d = desc.as(HistoricalAgentDescription.class).getDatePeriods().iterator().next();
        assertEquals("1933", d.getStartDate());
        assertEquals("1944", d.getEndDate());
    }

    @Test
    public void testImportItemsAlgemeyner() throws Exception {
        String eacFile = "algemeyner-yidisher-arbeter-bund-in-lite-polyn-un-rusland.xml";
        String itemId = "159#object";
        String itemDesc = "159";

        String logMessage = "Importing EAC " + eacFile;

        int count = getNodeCount(graph);
        logger.debug("count of nodes before importing: " + count);

        List<VertexProxy> before = GraphTestBase.getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(eacFile);
        ImportLog log = saxImportManager(EacImporter.class, EacHandler.class)
                .withScope(SystemScope.getInstance())
                .importInputStream(ios, logMessage);

        List<VertexProxy> after = GraphTestBase.getGraphState(graph);
        GraphTestBase.diffGraph(before, after).printDebug(System.err);
        // How many new nodes will have been created? We should have
        // - 1 more HistoricalAgent
        // - 1 more HistoricalAgentDescription
        // - 1 property
        // - 2 more MaintenanceEvent 
        // - 2 more linkEvents (1 for the HistoricalAgent, 1 for the User)
        // - 1 more SystemEvent
        assertEquals(count + 8, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY,
                itemId);
        assertTrue(docs.iterator().hasNext());
        HistoricalAgent unit = graph.frame(
                getVertexByIdentifier(graph, itemId),
                HistoricalAgent.class);

        // check the child items
        HistoricalAgentDescription c1 = graph.frame(
                getVertexByIdentifier(graph, itemDesc),
                HistoricalAgentDescription.class);
        assertEquals(Entities.HISTORICAL_AGENT_DESCRIPTION, c1.getType());

        assertTrue(c1.getProperty(Ontology.NAME_KEY) instanceof String);
        assertNotNull(c1.getProperty("otherFormsOfName"));
        // Alt names should be an array
        assertFalse(c1.getProperty("otherFormsOfName") instanceof String);

        // Ensure that c1 is a description of the unit
        for (Description d : unit.getDescriptions()) {
            assertEquals(d.getName(), c1.getName());
            assertEquals(d.getEntity().getId(), unit.getId());
        }

        // Check we've only got one action
        assertEquals(1, log.getCreated());
        SystemEvent ev = actionManager.getLatestGlobalEvent();
        assertEquals(logMessage, ev.getLogMessage());

        // Ensure the import action has the right number of subjects.
        List<Accessible> subjects = toList(ev.getSubjects());
        assertEquals(1, subjects.size());
        assertEquals(log.getChanged(), subjects.size());
    }
}
