package eu.ehri.project.commands;

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for GraphML dump. NB: Unlike the {@link eu.ehri.project.commands.GraphSON}
 * command loading the dump file will not result in an unchanged graph due to
 * lack of support for array properties.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class GraphMLTest extends AbstractFixtureTest {
    @Test
    public void testSaveDump() throws Exception {
        File temp = File.createTempFile("temp-file-name", ".xml");
        temp.deleteOnExit();
        assertEquals(0L, temp.length());

        String filePath = temp.getAbsolutePath();
        String[] outArgs = new String[]{filePath};

        GraphML export = new GraphML();
        CommandLine outCmdLine = export.getCmdLine(outArgs);
        assertEquals(0, export.execWithOptions(graph, outCmdLine));
        assertTrue(temp.length() > 0L);
    }
}
