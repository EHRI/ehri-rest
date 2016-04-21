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
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UshmmTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(UshmmTest.class);
    
    protected final String SINGLE_EAD = "irn44515.xml";
    protected final String IMPORTED_ITEM_ID = "irn44645";
    protected final String IMPORTED_ITEM_ALT_ID = "RG-50.586*0032";

    // Depends on fixtures
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItemsT() throws Exception {
        Repository agent = manager.getEntity(TEST_REPO, Repository.class);
        final String logMessage = "Importing a single EAD by UshmmTest";

        int origCount = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new SaxImportManager(graph, agent, validUser, true, false,
                    EadImporter.class, UshmmHandler.class)
                .importFile(ios, logMessage);

        printGraph(graph);
        /* How many new nodes will have been created? We should have
        * 2 more DocumentaryUnit
        * 2 more DocumentDescription
        * 1 more DatePeriod
        * 3 more import Event links
        * 1 more import Event
        * 17 more AccessPoints
        * 2 more MaintenanceEvent (creation)
        */
        int createCount = origCount + 28;
        assertEquals(createCount, getNodeCount(graph));

        // Yet we've only created 2 *logical* item...
        assertEquals(2, log.getCreated());

        Iterable<Vertex> docs = graph.getVertices("identifier", IMPORTED_ITEM_ID);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit unit = graph.frame(docs.iterator().next(), DocumentaryUnit.class);
        for(Description d : unit.getDocumentDescriptions()) {
            assertEquals("Oral history interview with Dobrila Kukolj", d.getName());
        	assertEquals("eng", d.getLanguageOfDescription());
        }
        SystemEvent event = unit.getLatestEvent();
        if (event != null) {
            logger.debug("event: " + event.getLogMessage());
        }
        
        // Check the alternative ID was added
        boolean foundAltId = false;
        for(String altId : unit.<List<String>>getProperty("otherIdentifiers")) {
        	if (altId.equals(IMPORTED_ITEM_ALT_ID)) {
        		foundAltId = true;
        		break;
        	}
        }
        assertTrue(foundAltId);

        List<SystemEvent> actions = toList(unit.getHistory());
        // Check we've only got one action
        assertEquals(1, actions.size());
        assertEquals(logMessage, actions.get(0).getLogMessage());

        // Check scope is correct...
        assertEquals(agent, unit.getAncestors().iterator().next().getPermissionScope());

        // Now re-import the same file
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log2 = new SaxImportManager(graph, agent, validUser, false, true, EadImporter.class,
                UshmmHandler.class).importFile(ios2, logMessage);

        // We should only have three more nodes, for
        // the action and
        // the user event links,
        // plus the global event
        assertEquals(createCount, getNodeCount(graph));
        // And one logical item should've been updated
        assertEquals(2, log2.getUnchanged());
    }
}
