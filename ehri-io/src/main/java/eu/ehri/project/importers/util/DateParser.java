package eu.ehri.project.importers.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
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


    /**
     * Extract a set of dates from input data. The input data is mutated to
     * remove the raw data.
     *
     * @param data a map of input data
     * @return a list of parsed date period maps
     */
    static List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = Lists.newArrayList();

        if (data.containsKey(Entities.DATE_PERIOD)) {
            Object dateRep = data.get(Entities.DATE_PERIOD);
            if (dateRep instanceof List) {
                for (Map<String, Object> event : (List<Map<String, Object>>) dateRep) {
                    extractedDates.add(getSubNode(event));
                }
            } else if (dateRep instanceof Map) {
                extractedDates.add(getSubNode((Map<String, Object>) dateRep));
            } else {
                logger.warn("Found a DatePeriod sub-node with unexpected type: " + dateRep);
            }
            data.remove(Entities.DATE_PERIOD);
        }

        Map<String, String> dateValues = returnDatesAsString(data);
        for (String s : dateValues.keySet()) {
            extractDate(s).ifPresent(extractedDates::add);
        }
        replaceDates(data, extractedDates, dateValues);

        return extractedDates;
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
                data.put(Ontology.DATE_PERIOD_START_DATE, normaliseDate(matcher.group(1)));
                data.put(Ontology.DATE_PERIOD_END_DATE, normaliseDate(matcher.group(matcher.groupCount() > 1 ? 2 : 1), true));
                data.put(Ontology.DATE_HAS_DESCRIPTION, date);
                break;
            }
        }
        return data;
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
                ParsePosition p = new ParsePosition(0);
                yearMonthDateFormat.parse(date, p);
                if (p.getIndex() > 0) {
                    returnDate = isoDateTimeFormat.print(DateTime.parse(date).plusMonths(1).minusDays(1));
                } else {
                    p = new ParsePosition(0);
                    yearDateFormat.parse(date, p);
                    if (p.getIndex() > 0) {
                        returnDate = isoDateTimeFormat.print(DateTime.parse(date).plusYears(1).minusDays(1));
                    }
                }
            }
        }
        return returnDate;
    }
}
