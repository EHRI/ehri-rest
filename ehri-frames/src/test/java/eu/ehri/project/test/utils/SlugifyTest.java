package eu.ehri.project.test.utils;

import eu.ehri.project.utils.Slugify;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: michaelb
 * Date: 22/02/13
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
public class SlugifyTest {

    @Test
    public void testSlugify() {
        String dontChange = "foo-bar";
        assertEquals(dontChange, Slugify.slugify(dontChange));
    }

    @Test
    public void removeSlashes() {
        String bad = "foo/bar";
        assertEquals("foo-bar", Slugify.slugify(bad));
    }
}
