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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Import utility class.
 */
public class Helpers {

    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    public static final String LANGUAGE_KEY_PREFIX = "language";

    private static final Pattern cDataReplace = Pattern.compile("\\]\\]>");

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
        if (value == null)
            return;
        String valuetrimmed = value.trim();
        if (valuetrimmed.isEmpty()) {
            return;
        }

        // Language properties

        if (property.startsWith(LANGUAGE_KEY_PREFIX)) {
            valuetrimmed = LanguageHelpers.iso639DashTwoCode(valuetrimmed);
        }

        valuetrimmed = StringUtils.normalizeSpace(valuetrimmed);

        LanguageHelpers.logger.debug("putProp: " + property + " " + valuetrimmed);

        Object propertyList;
        if (c.containsKey(property)) {
            propertyList = c.get(property);
            if (propertyList instanceof List) {
                ((List<Object>) propertyList).add(valuetrimmed);
            } else {
                List<Object> o = Lists.newArrayList();
                o.add(c.get(property));
                o.add(valuetrimmed);
                c.put(property, o);
            }
        } else {
            c.put(property, valuetrimmed);
        }
    }

    /**
     * Remove substrings that can not exist within an XML CDATA section.
     *
     * @param data the input string
     * @return a string with CDATA escapes removed
     */
    public static String escapeCData(String data) {
        return cDataReplace.matcher(data).replaceAll("");
    }
}
