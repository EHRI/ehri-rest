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
