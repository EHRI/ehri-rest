/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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

/**
 * Class for handling slugification of strings, for use
 * in global identifiers.
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
     *
     * @param input the String to slugify
     * @param replacement the replacement string
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

