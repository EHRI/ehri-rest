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
	public void testExecWithOptions() throws ParseException {
		String[] args = new String[]{"useradd","ben","--group","admin"};
		
		CommandLine cmdLine = new PosixParser().parse(new Options(), args);
		
		try {
			assertEquals(0, new UserAdd().execWithOptions(graph, cmdLine));
		} catch (ItemNotFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ValidationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionDenied e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeserializationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
