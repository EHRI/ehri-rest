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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.csv;

import com.google.common.collect.Lists;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class CsvAuthItemImporterTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(CsvAuthItemImporterTest.class);
    protected final String SINGLE_CSV = "wien_victims.csv";

    @Test
    public void testImportItems() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getEntity("auths", AuthoritativeSet.class);
        int vocCount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, vocCount);
        logger.debug("number of items: " + vocCount);

        final String logMessage = "Importing some subjects";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_CSV);

        List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = new CsvImportManager(graph, authoritativeSet, validUser,
                false, false, "eng", CsvAuthoritativeItemImporter.class).importInputStream(ios, logMessage);
        assertEquals(9, log.getCreated());
        printGraph(graph);

        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
        System.out.println(Lists.newArrayList(authoritativeSet.getAuthoritativeItems()));
        /*
         * 9 Item
         * 9 ItemDesc
         * 10 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 29, getNodeCount(graph));
        assertEquals(vocCount + 9, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (Accessible subject : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
    }
}
