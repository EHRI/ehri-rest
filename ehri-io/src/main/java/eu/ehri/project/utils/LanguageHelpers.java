package eu.ehri.project.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * Utilities for language code conversions.
 */
public class LanguageHelpers {

    private static final Map<String, Locale> locale2Map;
    private static final Map<String, String> locale3Map;
    private static final Map<String, Locale> localeNameMap;

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

    // Splitter for breaking up codes
    private static final Splitter codeSplitter = Splitter.on("-").omitEmptyStrings().limit(2);

    /**
     * Hack around missing country codes in JDK8
     */
    private static final ImmutableBiMap<String, String> countryCodes = ImmutableBiMap.<String, String>builder()
            .put("xk", "Kosovo")
            .put("mk", "North Macedonia")
            .build();

    /**
     * Limited selection of bibliographical 3-letter codes for the languages
     * we're most likely to run into, and their mappings to ISO639-2 Term codes.
     */
    private static final ImmutableBiMap<String, String> iso639BibTermLookup = ImmutableBiMap.<String, String>builder()
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

    /**
     * Continent names as defined by the EAG schema
     */
    private static final ImmutableBiMap<String, String> continentCodes = ImmutableBiMap.<String, String>builder()
            .put("AF", "Africa")
            .put("AN", "Antarctica")
            .put("AS", "Asia")
            .put("EU", "Europe")
            .put("NA", "North America")
            .put("OC", "Australia")
            .put("SA", "South America")
            .build();

    private static final ImmutableMap<String, String> countryCodesToContinents = ImmutableMap.<String, String>builder()
            .put("AD", "EU")
            .put("AE", "AS")
            .put("AF", "AS")
            .put("AG", "NA")
            .put("AI", "NA")
            .put("AL", "EU")
            .put("AM", "AS")
            .put("AN", "NA")
            .put("AO", "AF")
            .put("AP", "AS")
            .put("AQ", "AN")
            .put("AR", "SA")
            .put("AS", "OC")
            .put("AT", "EU")
            .put("AU", "OC")
            .put("AW", "NA")
            .put("AX", "EU")
            .put("AZ", "AS")
            .put("BA", "EU")
            .put("BB", "NA")
            .put("BD", "AS")
            .put("BE", "EU")
            .put("BF", "AF")
            .put("BG", "EU")
            .put("BH", "AS")
            .put("BI", "AF")
            .put("BJ", "AF")
            .put("BL", "NA")
            .put("BM", "NA")
            .put("BN", "AS")
            .put("BO", "SA")
            .put("BR", "SA")
            .put("BS", "NA")
            .put("BT", "AS")
            .put("BV", "AN")
            .put("BW", "AF")
            .put("BY", "EU")
            .put("BZ", "NA")
            .put("CA", "NA")
            .put("CC", "AS")
            .put("CD", "AF")
            .put("CF", "AF")
            .put("CG", "AF")
            .put("CH", "EU")
            .put("CI", "AF")
            .put("CK", "OC")
            .put("CL", "SA")
            .put("CM", "AF")
            .put("CN", "AS")
            .put("CO", "SA")
            .put("CR", "NA")
            .put("CU", "NA")
            .put("CV", "AF")
            .put("CX", "AS")
            .put("CY", "AS")
            .put("CZ", "EU")
            .put("DE", "EU")
            .put("DJ", "AF")
            .put("DK", "EU")
            .put("DM", "NA")
            .put("DO", "NA")
            .put("DZ", "AF")
            .put("EC", "SA")
            .put("EE", "EU")
            .put("EG", "AF")
            .put("EH", "AF")
            .put("ER", "AF")
            .put("ES", "EU")
            .put("ET", "AF")
            .put("EU", "EU")
            .put("FI", "EU")
            .put("FJ", "OC")
            .put("FK", "SA")
            .put("FM", "OC")
            .put("FO", "EU")
            .put("FR", "EU")
            .put("FX", "EU")
            .put("GA", "AF")
            .put("GB", "EU")
            .put("GD", "NA")
            .put("GE", "AS")
            .put("GF", "SA")
            .put("GG", "EU")
            .put("GH", "AF")
            .put("GI", "EU")
            .put("GL", "NA")
            .put("GM", "AF")
            .put("GN", "AF")
            .put("GP", "NA")
            .put("GQ", "AF")
            .put("GR", "EU")
            .put("GS", "AN")
            .put("GT", "NA")
            .put("GU", "OC")
            .put("GW", "AF")
            .put("GY", "SA")
            .put("HK", "AS")
            .put("HM", "AN")
            .put("HN", "NA")
            .put("HR", "EU")
            .put("HT", "NA")
            .put("HU", "EU")
            .put("ID", "AS")
            .put("IE", "EU")
            .put("IL", "AS")
            .put("IM", "EU")
            .put("IN", "AS")
            .put("IO", "AS")
            .put("IQ", "AS")
            .put("IR", "AS")
            .put("IS", "EU")
            .put("IT", "EU")
            .put("JE", "EU")
            .put("JM", "NA")
            .put("JO", "AS")
            .put("JP", "AS")
            .put("KE", "AF")
            .put("KG", "AS")
            .put("KH", "AS")
            .put("KI", "OC")
            .put("KM", "AF")
            .put("KN", "NA")
            .put("KP", "AS")
            .put("KR", "AS")
            .put("KW", "AS")
            .put("KY", "NA")
            .put("KZ", "AS")
            .put("LA", "AS")
            .put("LB", "AS")
            .put("LC", "NA")
            .put("LI", "EU")
            .put("LK", "AS")
            .put("LR", "AF")
            .put("LS", "AF")
            .put("LT", "EU")
            .put("LU", "EU")
            .put("LV", "EU")
            .put("LY", "AF")
            .put("MA", "AF")
            .put("MC", "EU")
            .put("MD", "EU")
            .put("ME", "EU")
            .put("MF", "NA")
            .put("MG", "AF")
            .put("MH", "OC")
            .put("MK", "EU")
            .put("ML", "AF")
            .put("MM", "AS")
            .put("MN", "AS")
            .put("MO", "AS")
            .put("MP", "OC")
            .put("MQ", "NA")
            .put("MR", "AF")
            .put("MS", "NA")
            .put("MT", "EU")
            .put("MU", "AF")
            .put("MV", "AS")
            .put("MW", "AF")
            .put("MX", "NA")
            .put("MY", "AS")
            .put("MZ", "AF")
            .put("NA", "AF")
            .put("NC", "OC")
            .put("NE", "AF")
            .put("NF", "OC")
            .put("NG", "AF")
            .put("NI", "NA")
            .put("NL", "EU")
            .put("NO", "EU")
            .put("NP", "AS")
            .put("NR", "OC")
            .put("NU", "OC")
            .put("NZ", "OC")
            .put("O1", "--")
            .put("OM", "AS")
            .put("PA", "NA")
            .put("PE", "SA")
            .put("PF", "OC")
            .put("PG", "OC")
            .put("PH", "AS")
            .put("PK", "AS")
            .put("PL", "EU")
            .put("PM", "NA")
            .put("PN", "OC")
            .put("PR", "NA")
            .put("PS", "AS")
            .put("PT", "EU")
            .put("PW", "OC")
            .put("PY", "SA")
            .put("QA", "AS")
            .put("RE", "AF")
            .put("RO", "EU")
            .put("RS", "EU")
            .put("RU", "EU")
            .put("RW", "AF")
            .put("SA", "AS")
            .put("SB", "OC")
            .put("SC", "AF")
            .put("SD", "AF")
            .put("SE", "EU")
            .put("SG", "AS")
            .put("SH", "AF")
            .put("SI", "EU")
            .put("SJ", "EU")
            .put("SK", "EU")
            .put("SL", "AF")
            .put("SM", "EU")
            .put("SN", "AF")
            .put("SO", "AF")
            .put("SR", "SA")
            .put("ST", "AF")
            .put("SV", "NA")
            .put("SY", "AS")
            .put("SZ", "AF")
            .put("TC", "NA")
            .put("TD", "AF")
            .put("TF", "AN")
            .put("TG", "AF")
            .put("TH", "AS")
            .put("TJ", "AS")
            .put("TK", "OC")
            .put("TL", "AS")
            .put("TM", "AS")
            .put("TN", "AF")
            .put("TO", "OC")
            .put("TR", "EU")
            .put("TT", "NA")
            .put("TV", "OC")
            .put("TW", "AS")
            .put("TZ", "AF")
            .put("UA", "EU")
            .put("UG", "AF")
            .put("UM", "OC")
            .put("US", "NA")
            .put("UY", "SA")
            .put("UZ", "AS")
            .put("VA", "EU")
            .put("VC", "NA")
            .put("VE", "SA")
            .put("VG", "NA")
            .put("VI", "NA")
            .put("VN", "AS")
            .put("VU", "OC")
            .put("WF", "OC")
            .put("WS", "OC")
            .put("YE", "AS")
            .put("YT", "AF")
            .put("ZA", "AF")
            .put("ZM", "AF")
            .put("ZW", "AF")
            .build();

    public static Optional<String> countryCodeToContinent(String countryCode) {
        String continentCode = countryCodesToContinents.get(countryCode.toUpperCase());
        if (continentCode != null) {
            return Optional.ofNullable(continentCodes.get(continentCode));
        }
        return Optional.empty();
    }

    /**
     * Get the best description for a given language code.
     *
     * @param item         a described item
     * @param priorDescOpt if the object is hierarchical, the parent-level
     *                     description
     * @param langCode     a 3-letter language code.
     * @return the best matching description found
     */
    public static Optional<Description> getBestDescription(Described item, Optional<Description> priorDescOpt, String langCode) {
        List<Description> descriptions = Lists.newArrayList(item.getDescriptions());
        descriptions.sort(Comparator.comparing(Entity::getId));
        Description fallBack = null;
        for (Description description : descriptions) {
            if (fallBack == null) {
                fallBack = description;
            }
            // First of all, check the description code (usually set to the
            // EAD file ID.) If this is the same as the parent, return the
            // current description.
            for (Description parent : priorDescOpt.map(Collections::singleton).orElse(Collections.emptySet())) {
                for (String code : Optional.ofNullable(parent.getDescriptionCode())
                        .map(Collections::singleton).orElse(Collections.emptySet())) {
                    if (code.equals(description.getDescriptionCode())) {
                        return Optional.of(description);
                    }
                }
            }

            // Otherwise, fall back to the first one with the same language
            if (description.getLanguageOfDescription().equalsIgnoreCase(langCode)) {
                return Optional.of(description);
            }
        }
        return Optional.ofNullable(fallBack);
    }

    public static Optional<Description> getBestDescription(Described item, String langCode) {
        return getBestDescription(item, Optional.empty(), langCode);
    }

    /**
     * Take an ISO-639-1 code or a language name and try and map to a valid ISO639-2 code.
     *
     * @param nameOrCode a language code or name to convert
     * @return the ISO 639-2 language code for that code or name, or the input string if
     * no conversion was found
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
     * no conversion was found
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

    /**
     * Convert a 2- or 3-letter language code to its (English) name.
     * <p>
     * If no name can be found the input code will be returned.
     *
     * @param code a 2- or 3-letter ISO639 code
     * @return a language name, with the original code as a fall back
     */
    public static String codeToName(String code) {
        if (code.length() == 2 && locale2Map.containsKey(code)) {
            return locale2Map.get(code).getDisplayLanguage(Locale.ENGLISH);
        } else if (code.length() == 3) {
            String termCode = iso639BibTermLookup.getOrDefault(code, code);
            String twoCode = locale3Map.get(termCode);
            if (locale2Map.containsKey(twoCode)) {
                return locale2Map.get(twoCode).getDisplayLanguage(Locale.ENGLISH);
            }
        }
        return code;
    }

    /**
     * Convert an ISO 3166-1 country code to its (English) name.
     *
     * @param code the 2-letter country code
     * @return the country name, or the code as a fallback
     */
    public static String countryCodeToName(String code) {
        if (countryCodes.containsKey(code)) {
            return countryCodes.get(code);
        } else {
            return new java.util.Locale(Locale.ENGLISH.getLanguage(), code)
                    .getDisplayCountry(Locale.ENGLISH);
        }
    }
}
