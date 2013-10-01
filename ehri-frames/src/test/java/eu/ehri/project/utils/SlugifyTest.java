package eu.ehri.project.utils;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test slugification.
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

    @Test
    public void removeMultiDashes() {
        String bad = "foo---bad";
        assertEquals("foo-bad", Slugify.slugify(bad));
    }
}
