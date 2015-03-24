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
    public void removeLeadingTrailingReplacements() {
        String bad = "-foo-bad-";
        assertEquals("foo-bad", Slugify.slugify(bad));
    }

    @Test
    public void removeMultipleLeadingTrailingReplacements() {
        String bad = "__foo-bad__";
        assertEquals("foo_bad", Slugify.slugify(bad, "_"));
    }

    @Test
    public void removeMultiDashes() {
        String bad = "foo---bad";
        assertEquals("foo-bad", Slugify.slugify(bad));
    }

    @Test
    public void greekTransliteration() {
        String greek = "Καλημέρα κόσμε";
        assertEquals("καλημέρα-κόσμε", Slugify.slugify(greek));
    }

    @Test
    public void hebrewTransliteration() {
        String hebrew = "ארכיון יד ושם";
        assertEquals("ארכיון-יד-ושם", Slugify.slugify(hebrew));
    }

    @Test
    public void cyrillicTransliteration() {
        String cyrillic = "Кіровоградська районна";
        assertEquals("кіровоградська-районна", Slugify.slugify(cyrillic));
    }

    @Test
    public void removeUnsafeOrReservedChars() {
        String unsafe = "hello \"\'|:/?#[]@*+,;=%<>{}~-() world";
        assertEquals("hello-world", Slugify.slugify(unsafe));
    }
}
