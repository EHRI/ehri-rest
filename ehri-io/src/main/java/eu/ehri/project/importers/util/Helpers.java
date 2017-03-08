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

package eu.ehri.project.importers.util;

import com.google.common.collect.Lists;
import eu.ehri.project.utils.LanguageHelpers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Import utility class.
 */
public class Helpers {

    private static final Logger logger = LoggerFactory.getLogger(Helpers.class);

    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    private static final String LANGUAGE_KEY_PREFIX = "language";

    private static final Pattern cDataReplace = Pattern.compile("\\]\\]>");

    private static String normaliseValue(String property, String value) {
        String trimmedValue = StringUtils.normalizeSpace(value);
        // Language codes are converted to their 3-letter alternates
        return property.startsWith(LANGUAGE_KEY_PREFIX)
                ? LanguageHelpers.iso639DashTwoCode(trimmedValue)
                : trimmedValue;
    }

    public static void overwritePropertyInGraph(Map<String, Object> c, String property, String value) {
        String normValue = normaliseValue(property, value);
        if (normValue == null || normValue.isEmpty()) {
            return;
        }
        logger.debug("overwrite property: {} {}", property, normValue);
        c.put(property, normValue);
    }

    /**
     * Stores this property value pair in the given graph node representation.
     * If the value is effectively empty, nothing happens.
     * If the property already exists, it is added to the value list.
     *
     * @param c        a Map representation of a graph node
     * @param property the key to store the value for
     * @param value    the value to store
     */
    public static void putPropertyInGraph(Map<String, Object> c, String property, String value) {
        String normValue = normaliseValue(property, value);
        if (normValue == null || normValue.isEmpty()) {
            return;
        }
        logger.debug("putProp: {} -> {}", property, normValue);
        if (c.containsKey(property)) {
            Object currentValue = c.get(property);
            if (currentValue instanceof List) {
                ((List) currentValue).add(normValue);
            } else {
                c.put(property, Lists.newArrayList(currentValue, normValue));
            }
        } else {
            c.put(property, normValue);
        }
    }

    /**
     * Remove sub-strings that can not exist within an XML CDATA section.
     *
     * @param data the input string
     * @return a string with CDATA escapes removed
     */
    public static String escapeCData(String data) {
        return cDataReplace.matcher(data).replaceAll("");
    }
}
