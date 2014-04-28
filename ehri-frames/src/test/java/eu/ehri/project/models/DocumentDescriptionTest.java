package eu.ehri.project.models;

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DocumentDescriptionTest extends AbstractFixtureTest {
    @Test
    public void testGetUnit() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentDescription cd1 = manager.getFrame("cd1", DocumentDescription.class);
        assertEquals(c1, cd1.getEntity());
    }
}
