package eu.ehri.project.commands;

import static org.junit.Assert.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.Test;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.test.AbstractFixtureTest;

public class UserAddTest extends AbstractFixtureTest {

	@Test
	public void testExecWithOptions() throws Exception {
		String[] args = new String[]{"ben", "--group", "admin"};
		
		UserAdd ua = new UserAdd();
		CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(0, ua.execWithOptions(graph, cmdLine));
	}

}
