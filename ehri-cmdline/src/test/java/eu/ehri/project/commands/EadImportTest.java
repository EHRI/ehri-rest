package eu.ehri.project.commands;

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EadImportTest extends AbstractFixtureTest {
    @Test
    public void testEadImport() throws Exception {
        String eadFile = getFixtureFilePath("single-ead.xml");
        String[] args = new String[]{"--user", "mike", "--scope", "r1", eadFile};

        EadImport ua = new EadImport();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(0, ua.execWithOptions(graph, cmdLine));
    }
}
