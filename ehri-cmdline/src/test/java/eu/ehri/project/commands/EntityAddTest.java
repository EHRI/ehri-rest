package eu.ehri.project.commands;

import eu.ehri.project.models.Country;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import static eu.ehri.project.commands.CmdEntryPoint.RetCode.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EntityAddTest extends AbstractFixtureTest {

    @Test
    public void testEntityAdd() throws Exception {
        String[] args = new String[]{EntityClass.COUNTRY.getName(),
                "-P", "name=Elbonia", "-P", "identifier=el",
                "--user", "mike"};
        EntityAdd ua = new EntityAdd();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(OK.getCode(), ua.execWithOptions(graph, cmdLine));
        Country el = manager.getFrame("el", EntityClass.COUNTRY, Country.class);
        assertEquals("Elbonia", el.asVertex().getProperty("name"));
    }

    @Test
    public void testEntityAddWithBadType() throws Exception {
        String[] args = new String[]{EntityClass.ADDRESS.getName(),
                "-P", "name=Elbonia", "-P", "identifier=el",
                "--user", "mike"};
        EntityAdd ua = new EntityAdd();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(BAD_DATA.getCode(), ua.execWithOptions(graph, cmdLine));
    }
}
