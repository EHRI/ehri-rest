/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
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

package eu.ehri.project.importers.managers;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.test.IOHelpers;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SaxImportManagerTest extends AbstractImporterTest {

    @Test
    public void testImportInferHierarchy() throws Exception {
        final List<String> tsv = Lists.newArrayList(
                "1c\t\n",
                "1s\t1c\n",
                "1f\t1s\n",
                "2c\t\n"
        );
        URI hierarchyFile = hierarchyFileUri(Joiner.on("").join(tsv));
        JsonMapper mapper = new JsonMapper();

        final ImmutableMap<String, String> map = ImmutableMap.of(
               "1c.xml", Resources.getResource("infer1c.xml").toURI().toString(),
               "1s.xml", Resources.getResource("infer1s.xml").toURI().toString(),
               "1f.xml", Resources.getResource("infer1f.xml").toURI().toString(),
               "2c.xml", Resources.getResource("infer2c.xml").toURI().toString()
        );

        byte[] buf = mapper.writer().writeValueAsBytes(map);
        InputStream stream = new ByteArrayInputStream(buf);

        SaxImportManager importer = saxImportManager(EadImporter.class, EadHandler.class,
                ImportOptions.basic().withHierarchyUri(hierarchyFile));
        ImportLog log = importer.importJson(stream, "Testing Hierarchy Import");
        assertEquals(4, log.getCreated());

        String[] ids = {"nl-r1-1c", "nl-r1-1c-1s", "nl-r1-1c-1s-1f", "nl-r1-2c"};
        for (String id : ids) {
            assertThat(manager.getEntityUnchecked(id, DocumentaryUnit.class), notNullValue());
        }
    }

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

    private URI hierarchyFileUri(String data) throws Exception {
        File temp = File.createTempFile("hierarchy-file", ".tsv");
        temp.deleteOnExit();
        FileUtils.writeStringToFile(temp, data, Charsets.UTF_8);
        return temp.toURI();
    }
}