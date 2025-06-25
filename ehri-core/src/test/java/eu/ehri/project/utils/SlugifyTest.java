/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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
        String unsafe = "hello \"'|:/?#[]@*+,;=%<>{}~-() world";
        assertEquals("hello-world", Slugify.slugify(unsafe));
    }
}
