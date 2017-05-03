/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
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

package eu.ehri.project.importers.managers;

import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.test.IOHelpers;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;


public class SaxImportManagerTest extends AbstractImporterTest {

    @Test
    public void testImportZipArchive() throws Exception {
        SaxImportManager manager = saxImportManager(EadImporter.class, EadHandler.class);
        File temp = File.createTempFile("test-zip", ".zip");
        temp.deleteOnExit();
        IOHelpers.createZipFromResources(temp, "single-ead.xml", "hierarchical-ead.xml");
        ImportLog log = importArchive(manager, temp.toPath());
        assertEquals(6, log.getCreated());
        assertEquals(0, log.getUnchanged());
    }

    @Test
    public void testImportTarArchive() throws Exception {
        SaxImportManager manager = saxImportManager(EadImporter.class, EadHandler.class);
        File temp = File.createTempFile("test-tar", ".tar");
        temp.deleteOnExit();
        IOHelpers.createTarFromResources(temp, "single-ead.xml", "hierarchical-ead.xml");
        ImportLog log = importArchive(manager, temp.toPath());
        assertEquals(6, log.getCreated());
        assertEquals(0, log.getUnchanged());
    }

    private ImportLog importArchive(ImportManager manager, Path path) throws Exception {
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path));
             ArchiveInputStream archiveInputStream =
                     new ArchiveStreamFactory(StandardCharsets.UTF_8.displayName())
                             .createArchiveInputStream(bis)) {
            return manager.importArchive(archiveInputStream, "Test");
        }
    }
}