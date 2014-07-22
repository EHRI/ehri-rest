package eu.ehri.project.models.base;

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertNull;
/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DescriptionTest extends AbstractFixtureTest {
    @Test
    public void testGetCreationProcess() throws Exception {
        Description d1 = manager.getFrame("cd1", Description.class);
        assertNull(d1.getCreationProcess());
    }
}
