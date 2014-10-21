package eu.ehri.project.commands;

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserAddTest extends AbstractFixtureTest {

	@Test
	public void testExecWithOptions() throws Exception {
		String[] args = new String[]{"ben", "--group", "admin"};
		
		UserAdd ua = new UserAdd();
		CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(0, ua.execWithOptions(graph, cmdLine));
	}
}
