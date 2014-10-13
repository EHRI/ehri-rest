package eu.ehri.project.commands;

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class RdfExportTest extends AbstractFixtureTest {
    @Test
    public void testExportTurtle() throws Exception {
        File temp = File.createTempFile("temp-file-name", ".turtle");
        temp.deleteOnExit();
        assertEquals(0L, temp.length());

        String filePath = temp.getAbsolutePath();
        String[] outArgs = new String[]{"-f", "turtle", filePath};

        RdfExport export = new RdfExport();
        CommandLine outCmdLine = export.getCmdLine(outArgs);
        assertEquals(0, export.execWithOptions(graph, outCmdLine));
        assertTrue(temp.length() > 0L);
    }
}
