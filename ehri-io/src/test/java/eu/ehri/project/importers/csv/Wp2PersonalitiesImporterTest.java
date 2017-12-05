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

package eu.ehri.project.importers.csv;

import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.CsvImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Wp2PersonalitiesImporterTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2PersonalitiesImporterTest.class);
    protected final String SINGLE_EAD = "wp2_persons_bare.csv";

    @Test
    public void testImportItems() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getEntity("auths", AuthoritativeSet.class);
        int voccount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, voccount);
        logger.debug("number of items: " + voccount);

        final String logMessage = "Importing some WP2 Personalities records";
        XmlImportProperties p = new XmlImportProperties("wp2personalities.properties");
        assertTrue(p.containsProperty("id"));
        assertTrue(p.containsProperty("name"));


        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        new CsvImportManager(graph, authoritativeSet, validUser,
                false, false, PersonalitiesImporter.class)
                .importInputStream(ios, logMessage);

        /*
         * 16 HistAgent
         * 16 HistAgentDesc
         * -- 9 more DatePeriods not 
         * 17 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        printGraph(graph);
        assertEquals(count + 50, getNodeCount(graph));
        assertEquals(voccount + 16, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (Accessible subject : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
    }
}
