package eu.ehri.project.commands;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EntityDeleteTest extends AbstractFixtureTest {
    @Test(expected = ItemNotFound.class)
    public void testExecWithOptions() throws Exception {
        String[] args = new String[]{"delete","reto","--user","mike", "--log", "Goodbye, Reto"};

        EntityDelete del = new EntityDelete();
        del.setCustomOptions();
        CommandLine cmdLine = new PosixParser().parse(del.options, args);
        assertEquals(0, del.execWithOptions(graph, cmdLine));
        manager.getFrame("reto", UserProfile.class);
    }
}
