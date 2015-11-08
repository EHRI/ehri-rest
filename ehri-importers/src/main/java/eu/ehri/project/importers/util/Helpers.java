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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for converting language codes.
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Ben Companjen (https://github.com/bencomp)
 */
public class Helpers {

    private static final Logger logger = LoggerFactory.getLogger(Helpers.class);

    // Splitter to breaking up
    private static final Splitter codeSplitter = Splitter.on("-").omitEmptyStrings().limit(2);

    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    public static final String LANGUAGE_KEY_PREFIX = "language";
    /**
     * Limited selection of bibliographical 3-letter codes for the languages
     * we're most likely to run into, and their mappings to ISO639-2 Term codes.
     */
    private static final ImmutableMap<String, String> iso639BibTermLookup = ImmutableMap.<String, String>builder()
            .put("alb", "sqi") // albanian
            .put("arm", "hye") // armenian
            .put("baq", "eus") // basque
            .put("ger", "deu") // german
            .put("dut", "nld") // dutch
            .put("rum", "ron") // romanian
            .put("mac", "mkd") // macedonian
            .put("slo", "slk") // slovak
            .put("fre", "fra") // french
            .put("cze", "ces") // czech
            .build();

    private static final Map<String, Locale> locale2Map;
    private static final Map<String, String> locale3Map;
    private static final Map<String, Locale> localeNameMap;

    /**
     * Stores this property value pair in the given graph node representation.
     * If the value is effectively empty, nothing happens.
     * If the property already exists, it is added to the value list.
     *
     * @param c a Map representation of a graph node
     * @param property the key to store the value for
     * @param value the value to store
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
            valuetrimmed = iso639DashTwoCode(valuetrimmed);
        }

        valuetrimmed = StringUtils.normalizeSpace(valuetrimmed);

        logger.debug("putProp: " + property + " " + valuetrimmed);

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

    static {
        String[] languages = Locale.getISOLanguages();
        locale2Map = Maps.newHashMapWithExpectedSize(languages.length);
        locale3Map = Maps.newHashMapWithExpectedSize(languages.length);
        localeNameMap = Maps.newHashMapWithExpectedSize(languages.length);
        for (String language : languages) {
            Locale locale = new Locale(language);
            locale2Map.put(language, locale);
            locale3Map.put(locale.getISO3Language(), language);
            localeNameMap.put(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase(), locale);
        }

    }

    /**
     * Take an ISO-639-1 code or a language name and try and map to a valid ISO639-2 code.
     * 
     * @param nameOrCode a language code or name to convert
     * @return the ISO 639-2 language code for that code or name, or the input string if
     *         no conversion was found
     */
    public static String iso639DashTwoCode(String nameOrCode) {
        if (nameOrCode.length() == 2 && locale2Map.containsKey(nameOrCode)) {
            return locale2Map.get(nameOrCode).getISO3Language();
        } else if (nameOrCode.length() == 3 && iso639BibTermLookup.containsKey(nameOrCode)) {
            return iso639BibTermLookup.get(nameOrCode);
        } else if (nameOrCode.length() > 3 && localeNameMap.containsKey(nameOrCode.toLowerCase())) {
            return localeNameMap.get(nameOrCode.toLowerCase()).getISO3Language();
            /* FIXME the localeNameMap depends on locale and translating an 
             * English name to a code fails when executed on 
             * e.g. a server with non-English locale
             */
        }
        return nameOrCode;
    }

    /**
     * Take an ISO-639-2 code or a language name and try and map to a valid ISO639-1 code.
     *
     * @param nameOrCode a language code or name to convert
     * @return the ISO 639-1 language code for that code or name, or the input string if
     *         no conversion was found
     */
    public static String iso639DashOneCode(String nameOrCode) {
        if (nameOrCode.length() == 3 && locale3Map.containsKey(nameOrCode)) {
            return locale3Map.get(nameOrCode);
        } else if (nameOrCode.length() == 3 && iso639BibTermLookup.containsKey(nameOrCode)) {
            return locale3Map.get(iso639BibTermLookup.get(nameOrCode));
        } else if (nameOrCode.length() > 3 && localeNameMap.containsKey(nameOrCode.toLowerCase())) {
            return localeNameMap.get(nameOrCode.toLowerCase()).getLanguage();
        } else if (nameOrCode.length() > 2 && nameOrCode.contains("-")) {
            // Attempt to handle codes like 'heb-Hebr' and 'eng-Latn'
            List<String> parts = Lists.newArrayList(codeSplitter.split(nameOrCode));
            if (parts.size() == 1) {
                return iso639DashOneCode(parts.get(0));
            } else if (parts.size() == 2) {
                return iso639DashOneCode(parts.get(0)) + "-" + parts.get(1);
            }
        }
        return nameOrCode;
    }
}
