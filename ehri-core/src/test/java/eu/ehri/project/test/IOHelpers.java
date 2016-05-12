package eu.ehri.project.test;

import com.google.common.io.Resources;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void createZipFromResources(File file, String... resources)
            throws URISyntaxException, IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String resource : resources) {
                URL url = Resources.getResource(resource);
                String name = new File(url.toURI()).getAbsolutePath();
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
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void createTarFromResources(File file, String... resources)
            throws URISyntaxException, IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(fos)) {
            for (String resource : resources) {
                URL url = Resources.getResource(resource);
                tos.putArchiveEntry(new TarArchiveEntry(new File(url.toURI())));
                Resources.copy(url, tos);
                tos.closeArchiveEntry();
            }
        }
    }
}
