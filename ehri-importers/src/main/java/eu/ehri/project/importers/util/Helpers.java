package eu.ehri.project.importers.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Locale;
import java.util.Map;

/**
 * Created by michaelb on 07/06/13.
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
        final String[] languages = Locale.getISOLanguages();
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
