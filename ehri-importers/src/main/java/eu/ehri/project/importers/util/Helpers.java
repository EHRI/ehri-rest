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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Locale;
import java.util.Map;

/**
 * Utility class for converting language codes.
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Ben Companjen (https://github.com/bencomp)
 */
public class Helpers {

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
    private static final Map<String, Locale> localeNameMap;

    static {
        String[] languages = Locale.getISOLanguages();
        locale2Map = Maps.newHashMapWithExpectedSize(languages.length);
        localeNameMap = Maps.newHashMapWithExpectedSize(languages.length);
        for (String language : languages) {
            Locale locale = new Locale(language);
            locale2Map.put(language, locale);
            localeNameMap.put(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase(), locale);
        }

    }

    /**
     * Take a nameOrCode or a language name and try and map to a valid ISO639-2 nameOrCode.
     * 
     * @param nameOrCode a language code or name to convert
     * @return the ISO 639-2t language code for that code or name, or the input string if 
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
        // FAIL!!!
        return nameOrCode;
    }
}
