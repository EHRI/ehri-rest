package eu.ehri.project.utils;

import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang.StringUtils;

import java.text.Normalizer;

/**
 * Class for handling slugification of strings, for use
 * in global identifiers.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Slugify {

    private static final Transliterator transliterator
            = Transliterator.getInstance("Hebrew-Latin; Cyrillic-Latin; Greek-Latin; Latin-Ascii; Any-Lower");
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
        return normalize(input)
                .replaceAll("\\s+", replacement)            // whitespace
                .replaceAll(replacement + "+", replacement) // replacements
                .replaceAll("^\\W|\\W$", "")                // leading/trailing non alpha
                .toLowerCase();
    }

    private static String normalize(String input) {
        final String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(ret)) {
            return "";
        }

        final String trans = transliterator.transform(ret);
        return Normalizer.normalize(trans, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9- ]", DEFAULT_REPLACE);
    }
}

