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
     * we're most likely to run into. Java's Locale does not know about these.
     */
    private static final ImmutableMap<String, String> iso639BibTermLookup = ImmutableMap.<String, String>builder()
            .put("heb", "he")
            .put("ger", "de")
            .put("rum", "ro")
            .put("yid", "yi")
            .put("sqi", "sq")
            .put("dut", "nl")
            .put("slo", "sk")
            .put("fre", "fr")
            .put("cze", "cs").build();

    private static final String[] languages = Locale.getISOLanguages();
    private static final Map<String, Locale> locale3Map = Maps.newHashMapWithExpectedSize(languages.length);
    private static final Map<String, Locale> localeNameMap = Maps.newHashMapWithExpectedSize(languages.length);
    private static final Map<String, Locale> englishNameMap = Maps.newHashMapWithExpectedSize(languages.length);

    static {
        for (String language : languages) {
            Locale locale = new Locale(language);
            locale3Map.put(locale.getISO3Language(), locale);
            localeNameMap.put(locale.getDisplayLanguage(), locale);
            englishNameMap.put(locale.getDisplayLanguage(Locale.ENGLISH), locale);
        }

    }

    public static String iso639Three2Two(String threeLetterCode) {
        if (locale3Map.containsKey(threeLetterCode)) {
            return locale3Map.get(threeLetterCode).getLanguage();
        } else if (iso639BibTermLookup.containsKey(threeLetterCode)) {
            return iso639BibTermLookup.get(threeLetterCode);
        }
        // FAIL!!!
        return threeLetterCode;
    }

    public static String iso639Name2Two(String threeLetterCode) {
        if (localeNameMap.containsKey(threeLetterCode))
            return localeNameMap.get(threeLetterCode).getLanguage();
        return threeLetterCode;
    }
    
    /**
     * Get the ISO639-1 language code for an English language name.
     * @param englishName the language name in English
     * @return the corresponding ISO639-1 two-letter language code, or null if the language name is unrecognised.
     */
    public static String englishName2Two(String englishName) {
    	if (englishNameMap.containsKey(englishName)) {
    		return englishNameMap.get(englishName).getLanguage();
    	}
//    	else if() {
//    		
//    	}
    	return null;
    }
}
