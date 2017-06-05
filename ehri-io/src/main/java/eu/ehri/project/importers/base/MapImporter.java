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

package eu.ehri.project.importers.base;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer of Map based representations of documentary units, historical agents,
 * virtual collections and other entities. Does not implement the actual
 * import methods.
 */
public abstract class MapImporter extends AbstractImporter<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(MapImporter.class);

    // NB: Using English locale here to avoid ambiguities caused by system dependent
    // time zones such as: Cannot parse "1940-05-16": Illegal instant due to time zone
    // offset transition (Europe/Amsterdam)
    // https://en.wikipedia.org/wiki/UTC%2B00:20
    private static final DateTimeFormatter isoDateTimeFormat = ISODateTimeFormat.date()
            .withLocale(Locale.ENGLISH);

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
            Pattern.compile("^\\[(\\d{4})\\]$"), Pattern.compile("^(\\d{4})$"),
            Pattern.compile("^(\\d{2})th century$"),
            Pattern.compile("^\\s*(\\d{4})\\s*-\\s*(\\d{4})"),
            //bundesarchive: 1906/19
            Pattern.compile("^\\s*(\\d{4})/(\\d{2})"),
            Pattern.compile("^\\s*(\\d{4})\\s*/\\s*(\\d{4})"),
            Pattern.compile("^(\\d{4}-\\d{1,2})/(\\d{4}-\\d{1,2})"),
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})/(\\d{4}-\\d{1,2}-\\d{1,2})")
    };

    // NB: Not static yet since these objects aren't thread safe :(
    private final SimpleDateFormat yearMonthDateFormat = new SimpleDateFormat("yyyy-MM");
    private final SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
    private final XmlImportProperties dates = new XmlImportProperties("dates.properties");

    public MapImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, Actioner actioner, ImportLog log) {
        super(framedGraph, permissionScope, actioner, log);
    }

    /**
     * Extract datevalues from datamap
     *
     * @param data the data map
     * @return returns a List with the separated datevalues
     */
    protected static Map<String, String> returnDatesAsString(Map<String, Object> data, XmlImportProperties dates) {
        Map<String, String> datesAsString = Maps.newHashMap();
        Object value;
        for (Entry<String, Object> property : data.entrySet()) {
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

    /**
     * Extract a list of entity bundles for DatePeriods from the data,
     * attempting to parse the unitdate attribute.
     *
     * @param data the data map
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = Lists.newArrayList();
        Map<String, String> dateValues = returnDatesAsString(data, dates);
        for (String s : dateValues.keySet()) {
            extractDate(s).ifPresent(extractedDates::add);
        }
        return extractedDates;
    }

    /**
     * The dates that have been extracted to the extractedDates will be removed from the data map
     *
     * @param data           the data map
     * @param extractedDates the set of extracted dates
     */
    protected void replaceDates(Map<String, Object> data, List<Map<String, Object>> extractedDates) {
        Map<String, String> dateValues = returnDatesAsString(data, dates);
        Map<String, String> dateTypes = Maps.newHashMap();
        for (String dateValue : dateValues.keySet()) {
            dateTypes.put(dateValues.get(dateValue), null);
        }
        for (Map<String, Object> dateMap : extractedDates) {
            dateValues.remove(dateMap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
        //replace dates in data map
        for (String datevalue : dateValues.keySet()) {
            String dateType = dateValues.get(datevalue);
            logger.debug("{} -- {}", datevalue, dateType);
            if (dateTypes.containsKey(dateType) && dateTypes.get(dateType) != null) {
                dateTypes.put(dateType, dateTypes.get(dateType) + ", " + datevalue.trim());
            } else {
                dateTypes.put(dateType, datevalue.trim());
            }
        }
        for (String dateType : dateTypes.keySet()) {
            logger.debug("datetype {} -- {}", dateType, dateTypes.get(dateType));
            if (dateTypes.get(dateType) == null) {
                data.remove(dateType);
            } else {
                data.put(dateType, dateTypes.get(dateType));
            }
            logger.debug("" + data.get(dateType));
        }
    }

    /**
     * Extract an Iterable of representations of maintenance events from the itemData.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return a List of node representations of maintenance events (may be empty)
     */
    @Override
    public Iterable<Map<String, Object>> extractMaintenanceEvent(Map<String, Object> itemData) {
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : itemData.keySet()) {
            if (key.equals("maintenanceEvent") && itemData.get(key) instanceof List) {
                for (Map<String, Object> event : (List<Map<String, Object>>) itemData.get(key)) {
                    list.add(getMaintenanceEvent(event));
                }
            }
        }
        return list;
    }

    /**
     * Convert a representation of a maintenance event from the maintenanceEvent data,
     * using property names from MaintenanceEvent.
     *
     * @param event a Map of event properties
     * @return a correct node representation of a single maintenance event
     */
    @Override
    public Map<String, Object> getMaintenanceEvent(Map<String, Object> event) {
        Map<String, Object> me = Maps.newHashMap();
        for (Entry<String, Object> eventEntry : event.entrySet()) {
            // Hack for EAG 1 and 2012 compatibility - maps maintenance event
            // types from old to new values
            if (eventEntry.getKey().equals(Ontology.MAINTENANCE_EVENT_TYPE)) {
                me.put(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType
                        .withName((String) eventEntry.getValue()).toString());
            } else {
                me.put(eventEntry.getKey(), eventEntry.getValue());
            }
        }
        if (!me.containsKey(Ontology.MAINTENANCE_EVENT_TYPE)) {
            me.put(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType.updated.name());
        }
        return me;
    }

    /**
     * Attempt to extract some date periods. This does not currently put the dates into ISO form.
     *
     * @param date the data map
     * @return returns a Map with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    private Optional<Map<String, Object>> extractDate(String date) {
        Map<String, Object> data = matchDate(date);
        return data.isEmpty() ? Optional.empty() : Optional.of(data);
    }

    private Map<String, Object> matchDate(String date) {
        Map<String, Object> data = Maps.newHashMap();
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                logger.debug("matched {}", date);
                data.put(Ontology.DATE_PERIOD_START_DATE, normaliseDate(matcher.group(1)));
                data.put(Ontology.DATE_PERIOD_END_DATE, normaliseDate(matcher.group(matcher
                        .groupCount() > 1 ? 2 : 1), Ontology.DATE_PERIOD_END_DATE));
                data.put(Ontology.DATE_HAS_DESCRIPTION, date);
                break;
            }
        }
        return data;
    }

    private String normaliseDate(String date) {
        return normaliseDate(date, Ontology.DATE_PERIOD_START_DATE);
    }

    /**
     * Normalise a date in a string.
     *
     * @param date       a String date that needs formatting
     * @param beginOrEnd a string signifying whether this date is the begin of
     *                   a period or the end of a period
     * @return a String containing the formatted date.
     */
    protected String normaliseDate(String date, String beginOrEnd) {
        String returnDate = isoDateTimeFormat.print(DateTime.parse(date));
        if (returnDate.startsWith("00")) {
            returnDate = "19" + returnDate.substring(2);
            date = "19" + date;
        }
        if (Ontology.DATE_PERIOD_END_DATE.equals(beginOrEnd)) {
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
