package eu.ehri.project.views;

import eu.ehri.project.models.Address;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.*;

public class UtilitiesTest extends AbstractFixtureTest {
    private Utilities utilities;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        utilities = new Utilities(graph);
    }

    @Test
    public void testFindReplacePropertyValue() throws Exception {
        long count = utilities.findReplacePropertyValue(EntityClass.ADDRESS,
                "streetAddress", "Strand", "Drury Lane");
        assertEquals(1L, count);
        assertEquals("Drury Lane", manager.getFrame("ar2", Address.class)
                .asVertex().getProperty("streetAddress"));
    }

    @Test
    public void testFindReplacePropertyValueWithBadKeys() throws Exception {
        for (String badKey : new String[]{EntityType.ID_KEY, EntityType.TYPE_KEY}) {
            try {
                utilities.findReplacePropertyValue(EntityClass.ADDRESS, badKey, "foo", "bar");
                fail("Find/replace should have thrown an error with bad key: " + badKey);
            } catch (IllegalArgumentException e) {
                // okay
            }
        }
    }

    @Test
    public void testFindReplacePropertyValueRE() throws Exception {
        long count = utilities.findReplacePropertyValueRE(EntityClass.ADDRESS,
                "webpage", Pattern.compile("^http:"), "https:");
        assertEquals(3L, count);
        // Check the replacement works for both scalar and array values
        List<String> webPages = manager.getFrame("ar1", Address.class)
                .asVertex().getProperty("webpage");
        assertEquals("https://www.niod.nl", webPages.get(0));
        assertEquals("https://www.kcl.ac.uk", manager.getFrame("ar2", Address.class)
                .asVertex().getProperty("webpage"));

        // Illustrating what a sharp tool this is, the names of all documentary units
        // with the same thing...
        long allDocs = utilities.findReplacePropertyValueRE(EntityClass.DOCUMENT_DESCRIPTION,
                "name", Pattern.compile(".*"), "allTheSameName!");
        assertEquals(6, allDocs);
    }

    @Test(expected = PatternSyntaxException.class)
    public void testFindReplacePropertyValueREWithBadPattern() throws Exception {
        String badRe = "[http:"; // mismatched quotes...
        utilities.findReplacePropertyValueRE(EntityClass.ADDRESS,
                "webpage", Pattern.compile(badRe), "https:");
    }

    @Test
    public void testReplacePropertyName() throws Exception {
        long count = utilities.replacePropertyName(EntityClass.ADDRESS, "webpage", "url");
        assertEquals(2L, count);
        assertEquals("http://www.kcl.ac.uk", manager.getFrame("ar2", Address.class)
                .asVertex().getProperty("url"));
    }

    @Test
    public void testReplacePropertyNameWithBadKeys() throws Exception {
        for (String badKey : new String[]{EntityType.ID_KEY, EntityType.TYPE_KEY}) {
            try {
                utilities.replacePropertyName(EntityClass.ADDRESS, badKey, "foo");
                fail("Replace property name should have thrown an error with bad key: " + badKey);
            } catch (IllegalArgumentException e) {
                // okay
            }
        }
    }
}