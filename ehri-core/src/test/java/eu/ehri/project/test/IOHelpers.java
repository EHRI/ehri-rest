package eu.ehri.project.test;

import com.google.common.io.Resources;

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
     * Create a zip file containing the named rsources
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
}
