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

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GenericEadImporterTest extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {

        final String logMessage = "Importing a single EAD";

        int origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream("generic-ead.xml");
        saxImportManager(EadImporter.class, EadHandler.class, "ara.properties")
                .importInputStream(ios, logMessage);
        /*
         * Nodes created:
         *  - 1 unit
         *  - 1 description
         *  - 1 system event
         *  - 2 event link
         *  - 3 subject access points
         *  - 1 creator access point
         *  - 1 maintenance event
         *  - 1 date period
         */
        assertEquals(origCount + 11, getNodeCount(graph));
    }

    @Test(expected = ValidationError.class)
    public void testImportInvalidItem() throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream("invalid-ead.xml");
        saxImportManager(EadImporter.class, EadHandler.class)
                .importInputStream(ios, "Test invalid item import");
    }

    @Test
    public void testImportInvalidItemTolerant() throws Exception {
        int origCount = getNodeCount(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream("invalid-ead.xml");
        ImportOptions options = ImportOptions.basic().withTolerant(true);
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, options)
                .importInputStream(ios, "Test invalid item import");
        System.out.println(log.getErrors());
        assertEquals(1, log.getErrored());
        assertEquals(origCount, getNodeCount(graph));
    }

    @Test
    public void testImportWithLang() throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream("single-ead-multilang-deu.xml");
        ImportOptions options = ImportOptions.basic().withDefaultLang("fre");
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, options)
                .importInputStream(ios, "Test language import");
        assertEquals(1, log.getCreated());
        assertTrue(manager.exists("nl-r1-c00001.deu-2_deu"));
    }

    @Test
    public void testImportWithDefaultLang() throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream("single-ead-no-lang.xml");
        ImportOptions options = ImportOptions.basic().withDefaultLang("fre");
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, options)
                .importInputStream(ios, "Test language import");
        assertEquals(1, log.getCreated());
        assertTrue(manager.exists("nl-r1-c00001.fre-1_fre"));
    }
}
