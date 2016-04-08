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

import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.base.PermissionScope;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;


public class IcaAtomJohnCageTest extends AbstractImporterTest {

    protected final String JOHNCAGEXML = "johnCage.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml

    @Test
    public void testImportItemsT() throws Exception {

        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a single EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(JOHNCAGEXML);
        new SaxImportManager(graph, agent, validUser, IcaAtomEadImporter.class,
                IcaAtomEadHandler.class).importFile(ios, logMessage);
        printGraph(graph);

        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits
        // - 1 more DocumentDescription
        // - 1 more DatePeriod
        // - 1 more UnknownProperty
        // - 1 more creatorAccess relation        
        // - 2 more import Event links (1 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 4 UndeterminedRelationships
        assertEquals(count + 12, getNodeCount(graph));
    }
}
