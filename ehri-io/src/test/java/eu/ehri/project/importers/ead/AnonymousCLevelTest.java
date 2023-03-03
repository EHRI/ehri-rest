/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.ImportValidationError;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;


public class AnonymousCLevelTest extends AbstractImporterTest {

    @Test
    public void testImportItems() throws Exception {
        String ead = "anonymous-c-levels.xml";
        InputStream ios = ClassLoader.getSystemResourceAsStream(ead);
        try {
            saxImportManager(EadImporter.class, EadHandler.class).importInputStream(ios, "Test");
            fail("Import with " + ead + " should have thrown a validation error");
        } catch (ImportValidationError ex) {
            assertThat(ex.getError().getMessage(),
                    containsString("DocumentaryUnit p/c1/[null] - [Item completed prior to line: 29]: " +
                            "identifier: Parent item has missing identifier"));
        }
    }

    @Test
    public void testImportItemsTolerant() throws Exception {
        String ead = "anonymous-c-levels.xml";
        InputStream ios = ClassLoader.getSystemResourceAsStream(ead);
        ImportLog log = saxImportManager(EadImporter.class, EadHandler.class, ImportOptions.basic().withTolerant(true))
                .importInputStream(ios, "Test");
        System.out.println(log.getData());
        assertEquals(2, log.getCreated());
        assertEquals(2, log.getErrored());
    }
}
