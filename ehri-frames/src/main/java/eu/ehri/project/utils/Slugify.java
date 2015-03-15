package eu.ehri.project.utils;

/**
 * Class for handling slugification of strings, for use
 * in global identifiers.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Slugify {

    private static final String DEFAULT_REPLACE = "-";

    /**
     * Slugify the input string using the default replacement string.
     * @param input the String to slugify
     * @return the slugified copy of the input string
     */
    public static String slugify(String input) {
        return slugify(input, DEFAULT_REPLACE);
    }

    /**
     * Slugify the input string using the given replacement string.
     * @param input the String to slugify
     * @return the slugified copy of the input string
     */
    public static String slugify(String input, String replacement) {
        return input
                .replaceAll("[\\p{P}=+<>~\\|]", replacement)
                .replaceAll("\\s+", replacement)            // whitespace
                .replaceAll(replacement + "+", replacement) // replacements
                .replaceAll("^" + replacement + "|" + replacement + "$", "") // leading/trailing replacements
                .toLowerCase();
    }
}

