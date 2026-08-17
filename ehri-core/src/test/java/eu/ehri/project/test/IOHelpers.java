/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.test;

import com.google.common.io.Resources;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Test helpers for IO-related tasks.
 */
public class IOHelpers {
    /**
     * Create a zip file containing the named resources.
     *
     * @param file      a file object (typically a temp file)
     * @param resources the resource names
     */
    public static void createZipFromResources(File file, String... resources)
            throws URISyntaxException, IOException {
        try (OutputStream fos = Files.newOutputStream(file.toPath());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String resource : resources) {
                URL url = Resources.getResource(resource);
                String name = Paths.get(url.toURI()).normalize().toString();
                zos.putNextEntry(new ZipEntry(name));
                Resources.copy(url, zos);
                zos.closeEntry();
            }
        }
    }

    /**
     * Create a tar file containing the named resources.
     *
     * @param file      a file object (typically a temp file)
     * @param resources the resource names
     */
    public static void createTarFromResources(File file, String... resources)
            throws URISyntaxException, IOException {
        try (OutputStream fos = Files.newOutputStream(file.toPath());
             TarArchiveOutputStream tos = new TarArchiveOutputStream(fos)) {
            for (String resource : resources) {
                URL url = Resources.getResource(resource);
                tos.putArchiveEntry(new TarArchiveEntry(new File(url.toURI())));
                Resources.copy(url, tos);
                tos.closeArchiveEntry();
            }
        }
    }

    public static void gzipFile(Path in, Path out) throws IOException {
        try (InputStream fis = Files.newInputStream(in);
             OutputStream fos = Files.newOutputStream(out);
             GZIPOutputStream gzip = new GZIPOutputStream(fos)) {
            IOUtils.copy(fis, gzip);
        }
    }
}
