package eu.ehri.project.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void greekTransliteration() {
        String greek = "Καλημέρα κόσμε";
        assertEquals("kalemera-kosme", Slugify.slugify(greek));
    }

    @Test
    public void hebrewTransliteration() {
        String hebrew = "ארכיון יד ושם";
        assertEquals("rkywn-yd-wsm", Slugify.slugify(hebrew));
    }

    @Test
    public void cyrillicTransliteration() {
        String cyrillic = "Кіровоградська районна";
        assertEquals("kirovogradska-rajonna", Slugify.slugify(cyrillic));
    }
}
