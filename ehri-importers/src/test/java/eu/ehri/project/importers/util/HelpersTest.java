package eu.ehri.project.importers.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class HelpersTest {
    @Test
    public void testIso639DashTwoCode() throws Exception {
        // two-to-three
        assertEquals("sqi", Helpers.iso639DashTwoCode("sq"));
        // bibliographic to term
        assertEquals("sqi", Helpers.iso639DashTwoCode("alb"));
        // name to code
        assertEquals("eng", Helpers.iso639DashTwoCode("English"));
    }
}
