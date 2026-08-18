/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DatePeriod;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.ehri.project.importers.util.ImportHelpers.getSubNode;

/**
 * This class contains static functions to extract date information from
 * largely unstructured maps.
 *
 * There are two main scenarios:
 *
 *  - Pre-structured date periods, as found within EAD-3 daterange nodes. These
 *    are a map or list of maps with the key 'DatePeriod'
 *  - Unstructured text-based dates in formats we recognise as a range, keyed to
 *    either 'unitDates', 'creationDate', or 'existDate' (or others added to
 *    `dates.properties`.)
 *
 *  Notable, the function that returns the dates removes the data from
 *  which they were extracted
 */
class DateParser {

    private static final Logger logger = LoggerFactory.getLogger(DateParser.class);

    // Various date patterns
    private static final Pattern[] datePatterns = {
            // Yad Vashem, ICA-Atom style: 1924-1-1 - 1947-12-31
            // Yad Vashem in Wp2: 12-15-1941, 9-30-1944
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})\\s?-\\s?(\\d{4}-\\d{1,2}-\\d{1,2})$"),
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})$"),
            Pattern.compile("^(\\d{4})\\s?-\\s?(\\d{4})$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4}s)-\\[(\\d{4}s)\\]$"),
            Pattern.compile("^\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4})$"),
            Pattern.compile("^(\\d{2})th century$"),
            Pattern.compile("^\\s*(\\d{4})\\s*-\\s*(\\d{4})"),
            //bundesarchive: 1906/19
            Pattern.compile("^\\s*(\\d{4})/(\\d{2})"),
            Pattern.compile("^\\s*(\\d{4})\\s*/\\s*(\\d{4})"),
            Pattern.compile("^(\\d{4}-\\d{1,2})/(\\d{4}-\\d{1,2})"),
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})/(\\d{4}-\\d{1,2}-\\d{1,2})"),
            Pattern.compile("^(\\d{4})/(\\d{4}-\\d{1,2}-\\d{1,2})")
    };

    // NB: Using English locale here to avoid ambiguities caused by system dependent
    // time zones such as: Cannot parse "1940-05-16": Illegal instant due to time zone
    // offset transition (Europe/Amsterdam)
    // https://en.wikipedia.org/wiki/UTC%2B00:20
    private static final DateTimeFormatter isoDateTimeFormat = ISODateTimeFormat.date().withLocale(Locale.ENGLISH);

    // NB: Not static yet since these objects aren't thread safe :(
    private static final SimpleDateFormat yearMonthDateFormat = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
    private static final XmlImportProperties dates = new XmlImportProperties("dates.properties");

    // Temporary keys used to carry the EAD3 @standarddate attributes on a structured
    // date sub-node until they are reconciled into the actual start/end date properties.
    static final String START_STANDARD_DATE = "startStandardDate";
    static final String END_STANDARD_DATE = "endStandardDate";

    // Captured alongside unitDates text via ead2002.properties; EAD2002 has no
    // structured date sub-node, so this is reconciled positionally instead.
    private static final String UNIT_DATES = "unitDates";
    private static final String UNIT_DATES_NORMAL = "unitDatesNormal";

    // A date whose optional month/day groups reveal its granularity. Lenient about
    // digit count (1 or 2) so it also covers the sloppy text patterns above, e.g.
    // "1924-1-1", as well as the always-2-digit forms from structured sub-nodes.
    private static final Pattern DATE_COMPONENT = Pattern.compile("(\\d{4})(-\\d{1,2})?(-\\d{1,2})?");

    // A unitdate/@normal component: YYYY, YYYYMM or YYYYMMDD, with or without
    // hyphen separators (both "193904" and "1939-04" are accepted).
    private static final Pattern NORMAL_DATE_COMPONENT = Pattern.compile("(\\d{4})-?(\\d{2})?-?(\\d{2})?");

    /**
     * Extract a set of dates from input data. The input data is mutated to
     * remove the raw data.
     *
     * @param data a map of input data
     * @return a list of parsed date period maps, with exact duplicates collapsed
     */
    static List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = Lists.newArrayList();
        if (data.containsKey(Entities.DATE_PERIOD)) {
            Object dateRep = data.get(Entities.DATE_PERIOD);
            if (dateRep instanceof List) {
                for (Map<String, Object> dateData : (List<Map<String, Object>>) dateRep) {
                    extractedDates.add(normaliseStructuredDate(getSubNode(dateData)));
                }
            } else if (dateRep instanceof Map) {
                extractedDates.add(normaliseStructuredDate(getSubNode((Map<String, Object>) dateRep)));
            } else {
                logger.warn("Found a DatePeriod sub-node with unexpected type: " + dateRep);
            }
            data.remove(Entities.DATE_PERIOD);
        }

        extractNormalisedUnitDates(data, extractedDates);

        Map<String, String> dateValues = returnDatesAsString(data);
        for (String s : dateValues.keySet()) {
            extractDate(s).ifPresent(dateMap -> {
                // unitDates (ISAD(G) 3.1.3) is a creation date; exporters only
                // render dates of this type, so it must be set explicitly.
                if (UNIT_DATES.equals(dateValues.get(s))) {
                    dateMap.put(Ontology.DATE_PERIOD_TYPE, DatePeriod.DatePeriodType.creation.name());
                }
                extractedDates.add(dateMap);
            });
        }
        replaceDates(data, extractedDates, dateValues);

        // Collapse exact duplicates, e.g. a repeated <unitdate> element.
        return Lists.newArrayList(new LinkedHashSet<>(extractedDates));
    }

    private static void replaceDates(Map<String, Object> data, List<Map<String, Object>> extractedDates, Map<String, String> dateValues) {
        Map<String, String> dateTypes = Maps.newHashMap();
        for (String dateValue : dateValues.keySet()) {
            dateTypes.put(dateValues.get(dateValue), null);
        }
        for (Map<String, Object> dateMap : extractedDates) {
            dateValues.remove(dateMap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
        //replace dates in data map
        for (String dateValue : dateValues.keySet()) {
            String dateType = dateValues.get(dateValue);
            if (dateTypes.containsKey(dateType) && dateTypes.get(dateType) != null) {
                dateTypes.put(dateType, dateTypes.get(dateType) + ", " + dateValue.trim());
            } else {
                dateTypes.put(dateType, dateValue.trim());
            }
        }
        for (String dateType : dateTypes.keySet()) {
            if (dateTypes.get(dateType) == null) {
                data.remove(dateType);
            } else {
                data.put(dateType, dateTypes.get(dateType));
            }
        }
    }

    private static Optional<Map<String, Object>> extractDate(String date) {
        Map<String, Object> data = matchDate(date);
        return data.isEmpty() ? Optional.empty() : Optional.of(data);
    }

    private static Map<String, Object> matchDate(String date) {
        Map<String, Object> data = Maps.newHashMap();
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                data.put(Ontology.DATE_PERIOD_START_DATE, matcher.group(1));
                data.put(Ontology.DATE_PERIOD_END_DATE, matcher.group(matcher.groupCount() > 1 ? 2 : 1));
                data.put(Ontology.DATE_HAS_DESCRIPTION, date);
                break;
            }
        }
        // A pattern match that doesn't turn out to be a valid date (rare, but e.g.
        // the "Nth century" pattern isn't one) is rejected rather than kept half-
        // resolved, so the original text is preserved instead - same as @normal.
        return !data.isEmpty() && resolveDatePrecision(data) ? data : Collections.emptyMap();
    }

    /**
     * Reconcile unitdate/@normal values against the parallel unitDates text list,
     * only when the two are the same length - otherwise it's not safe to say
     * which text goes with which value, so all fall back to pattern-matching.
     *
     * @param data           mutable input data; matched entries are removed
     * @param extractedDates resolved date periods are appended here
     */
    private static void extractNormalisedUnitDates(Map<String, Object> data, List<Map<String, Object>> extractedDates) {
        if (!data.containsKey(UNIT_DATES_NORMAL)) {
            return;
        }
        List<String> normals = coerceToList(data.remove(UNIT_DATES_NORMAL));
        List<String> texts = data.containsKey(UNIT_DATES)
                ? coerceToList(data.get(UNIT_DATES))
                : Lists.newArrayList();
        if (texts.size() != normals.size()) {
            logger.debug("Mismatched unitDates ({}) and @normal ({}) counts, skipping normal-attribute date resolution",
                    texts.size(), normals.size());
            return;
        }

        List<String> unmatched = Lists.newArrayList();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            Optional<Map<String, Object>> parsed = parseNormalUnitDate(normals.get(i), text);
            if (parsed.isPresent()) {
                extractedDates.add(parsed.get());
            } else {
                unmatched.add(text);
            }
        }
        if (unmatched.isEmpty()) {
            data.remove(UNIT_DATES);
        } else {
            data.put(UNIT_DATES, unmatched.size() == 1 ? unmatched.get(0) : unmatched);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> coerceToList(Object value) {
        return value instanceof List ? (List<String>) value : Lists.newArrayList((String) value);
    }

    /**
     * Parse a unitdate/@normal value (a single date, or a "/"-separated range) into
     * a date period. Unlike the other callers of {@link #resolveDatePrecision}, an
     * unparseable value here isn't kept half-resolved - it's rejected outright, so
     * the caller can fall back to preserving the original unitdate text instead.
     *
     * @param normal      the raw normal attribute value
     * @param description the corresponding unitdate text
     * @return a date period map, or empty if unparseable
     */
    private static Optional<Map<String, Object>> parseNormalUnitDate(String normal, String description) {
        String[] parts = normal.trim().split("/", 2);
        Optional<String> start = canonicaliseNormalComponent(parts[0]);
        if (!start.isPresent()) {
            return Optional.empty();
        }
        Map<String, Object> data = Maps.newHashMap();
        data.put(Ontology.DATE_PERIOD_START_DATE, start.get());
        if (parts.length > 1) {
            Optional<String> end = canonicaliseNormalComponent(parts[1]);
            if (!end.isPresent()) {
                return Optional.empty();
            }
            data.put(Ontology.DATE_PERIOD_END_DATE, end.get());
        }
        data.put(Ontology.DATE_HAS_DESCRIPTION, description);
        // unitDates (ISAD(G) 3.1.3) is always a creation date; exporters only
        // render dates of this type, so it must be set explicitly.
        data.put(Ontology.DATE_PERIOD_TYPE, DatePeriod.DatePeriodType.creation.name());
        return resolveDatePrecision(data) ? Optional.of(data) : Optional.empty();
    }

    /**
     * Convert a unitdate/@normal component (YYYY, YYYYMM or YYYYMMDD, optionally
     * hyphenated) into the canonical hyphenated form expected by {@link #normaliseDate}.
     */
    private static Optional<String> canonicaliseNormalComponent(String component) {
        Matcher m = NORMAL_DATE_COMPONENT.matcher(component.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder(m.group(1));
        if (m.group(2) != null) {
            sb.append('-').append(m.group(2));
        }
        if (m.group(3) != null) {
            sb.append('-').append(m.group(3));
        }
        return Optional.of(sb.toString());
    }

    /**
     * Normalise a structured (e.g. EAD3 or EAC) date sub-node: replace start/end
     * date text with the @standarddate values if present, then resolve precision
     * and widen dates as usual. Tolerant of a bad value - there's no fallback for
     * a structured sub-node, so it's left as-is rather than dropped.
     *
     * @param node a mutable structured date sub-node
     * @return the same node, with precision resolved and temporary keys removed
     */
    private static Map<String, Object> normaliseStructuredDate(Map<String, Object> node) {
        moveIfPresent(node, START_STANDARD_DATE, Ontology.DATE_PERIOD_START_DATE);
        moveIfPresent(node, END_STANDARD_DATE, Ontology.DATE_PERIOD_END_DATE);
        resolveDatePrecision(node);
        return node;
    }

    private static void moveIfPresent(Map<String, Object> node, String from, String to) {
        Object value = node.remove(from);
        if (value != null) {
            node.put(to, value);
        }
    }

    /**
     * Resolve a date period's precision - an explicit value (e.g. EAD3's
     * @localtype) or one inferred from the start date's granularity - then widen
     * start/end to full dates. This is the single place a DatePeriod's start/end
     * become a full YYYY-MM-DD, whatever XML import path produced them.
     *
     * @param node a mutable map holding raw (possibly partial) start/end dates
     * @return true if every present date value was successfully widened
     */
    private static boolean resolveDatePrecision(Map<String, Object> node) {
        Optional<DatePeriod.DatePrecision> precision = parsePrecision(node.get(Ontology.DATE_PERIOD_PRECISION));
        if (!precision.isPresent()) {
            precision = inferPrecision(node.get(Ontology.DATE_PERIOD_START_DATE));
        }
        if (precision.isPresent()) {
            node.put(Ontology.DATE_PERIOD_PRECISION, precision.get().name());
        } else {
            node.remove(Ontology.DATE_PERIOD_PRECISION);
        }

        boolean start = widenIfPresent(node, Ontology.DATE_PERIOD_START_DATE, false);
        boolean end = widenIfPresent(node, Ontology.DATE_PERIOD_END_DATE, true);
        return start && end;
    }

    /**
     * Widen a date value in place to a full YYYY-MM-DD, if present.
     */
    private static boolean widenIfPresent(Map<String, Object> node, String key, boolean endOfPeriod) {
        Object value = node.get(key);
        if (value == null) {
            return true;
        }
        try {
            node.put(key, normaliseDate(value.toString().trim(), endOfPeriod));
            return true;
        } catch (IllegalArgumentException e) {
            logger.debug("Unable to widen date value: {}", value, e);
            return false;
        }
    }

    /**
     * Parse an explicit precision value (e.g. EAD3's @localtype). Unrecognised
     * values yield an empty result.
     */
    private static Optional<DatePeriod.DatePrecision> parsePrecision(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(DatePeriod.DatePrecision.valueOf(value.toString().trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<DatePeriod.DatePrecision> inferPrecision(Object date) {
        if (date == null) {
            return Optional.empty();
        }
        Matcher m = DATE_COMPONENT.matcher(date.toString().trim());
        if (!m.matches()) {
            return Optional.empty();
        } else if (m.group(3) != null) {
            return Optional.of(DatePeriod.DatePrecision.day);
        } else if (m.group(2) != null) {
            return Optional.of(DatePeriod.DatePrecision.month);
        }
        return Optional.of(DatePeriod.DatePrecision.year);
    }

    private static Map<String, String> returnDatesAsString(Map<String, Object> data) {
        Map<String, String> datesAsString = Maps.newHashMap();
        Object value;
        for (Map.Entry<String, Object> property : data.entrySet()) {
            if (dates.containsProperty(property.getKey()) && (value = property.getValue()) != null) {
                if (property.getValue() instanceof String) {
                    String dateValue = (String) value;
                    for (String d : dateValue.split(",")) {
                        datesAsString.put(d, property.getKey());
                    }
                } else if (property.getValue() instanceof List) {
                    for (String s : (List<String>) value) {
                        datesAsString.put(s, property.getKey());
                    }
                }
            }
        }
        return datesAsString;
    }

    static String normaliseDate(String date) {
        return normaliseDate(date, false);
    }

    /**
     * Normalise a date in a string.
     *
     * @param date        a String date that needs formatting
     * @param endOfPeriod a string signifying whether this date is the begin of
     *                    a period or the end of a period
     * @return a String containing the formatted date.
     */
    static String normaliseDate(String date, boolean endOfPeriod) {
        String returnDate = isoDateTimeFormat.print(DateTime.parse(date));
        if (returnDate.startsWith("00")) {
            returnDate = "19" + returnDate.substring(2);
            date = "19" + date;
        }
        if (endOfPeriod) {
            if (!date.equals(returnDate)) {
                // Require the full string consumed, not just p.getIndex() > 0 - otherwise
                // lenient parsing matches the "yyyy-MM" prefix of e.g. "1945-6-30" too.
                ParsePosition p = new ParsePosition(0);
                yearMonthDateFormat.parse(date, p);
                if (p.getIndex() == date.length()) {
                    returnDate = isoDateTimeFormat.print(DateTime.parse(date).plusMonths(1).minusDays(1));
                } else {
                    p = new ParsePosition(0);
                    yearDateFormat.parse(date, p);
                    if (p.getIndex() == date.length()) {
                        returnDate = isoDateTimeFormat.print(DateTime.parse(date).plusYears(1).minusDays(1));
                    }
                }
            }
        }
        return returnDate;
    }
}
