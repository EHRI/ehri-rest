package eu.ehri.project.utils;

/**
 * michaelb
 *
 * Adapted from:
 * https://github.com/slugify/slugify/blob/master/src/main/java/com/github/slugify/Slugify.java
 */

import org.apache.commons.lang.StringUtils;

import java.text.Normalizer;

public class Slugify {

    private static final String DEFAULT_REPLACE = "-";

    public static String slugify(String input) {
        return slugify(input, DEFAULT_REPLACE);
    }

    public static String slugify(String input, String replacement) {
        String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(input)) {
            return "";
        }

        ret = normalize(ret);
        ret = removeDuplicateWhiteSpaces(ret);
        return ret.replaceAll(" ", "-").replaceAll("-+", "-").toLowerCase();
    }

    private static String normalize(String input) {
        String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(ret)) {
            return "";
        }

      
        return Normalizer.normalize(ret, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9- ]", DEFAULT_REPLACE);
    }

    private static String removeDuplicateWhiteSpaces(String input) {
        String ret = StringUtils.trim(input);
        if (StringUtils.isBlank(ret)) {
            return "";
        }

        return ret.replaceAll("\\s+", " ");
    }
}

