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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.NodeProperties;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.utils.LanguageHelpers;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Import utility class.
 */
public class ImportHelpers {

    public static final String RESOLVED_LINK_DESC = "Link provided by data provider.";
    public static final String LINK_TARGET = "target";
    public static final String OBJECT_IDENTIFIER = "objectIdentifier";

    // Keys in the node that denote unknown properties must start with the value of UNKNOWN.
    public static final String UNKNOWN_PREFIX = "UNKNOWN_";
    private static final String NODE_PROPERTIES = "allowedNodeProperties.csv";

    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    private static final String LANGUAGE_KEY_PREFIX = "language";

    private static final Logger logger = LoggerFactory.getLogger(ImportHelpers.class);
    private static final Joiner stringJoiner = Joiner.on("\n\n").skipNulls();
    private static final NodeProperties nodeProperties = loadNodeProperties();

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
    private static final DateTimeFormatter isoDateTimeFormat = ISODateTimeFormat.date()
            .withLocale(Locale.ENGLISH);

    // NB: Not static yet since these objects aren't thread safe :(
    private static final SimpleDateFormat yearMonthDateFormat = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
    private static final XmlImportProperties dates = new XmlImportProperties("dates.properties");

    /**
     * Extract properties from the itemData Map that are marked as unknown, and return them in a new Map.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError never
     */
    public static Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = Maps.newHashMap();
        for (Map.Entry<String, Object> key : itemData.entrySet()) {
            if (key.getKey().startsWith(UNKNOWN_PREFIX)) {
                unknowns.put(key.getKey().substring(UNKNOWN_PREFIX.length()), key.getValue());
            }
        }
        return unknowns;
    }


    /**
     * only properties that have the multivalued-status can actually be multivalued. all other properties will be
     * flattened by this method.
     *
     * @param key    a property key
     * @param value  a property value
     * @param entity the EntityClass with which this frameMap must comply
     */
    public static Object flattenNonMultivaluedProperties(String key, Object value, EntityClass entity) {
        if (value instanceof List
                && !(nodeProperties.hasProperty(entity.getName(), key)
                && nodeProperties.isMultivaluedProperty(entity.getName(), key))) {
            logger.trace("Flattening array property value: {}: {}", key, value);
            return stringJoiner.join((List) value);
        } else {
            return value;
        }
    }

    /**
     * Extract DocumentaryUnit properties from the itemData and return them as a new Map.
     * This implementation only extracts the objectIdentifier.
     * <p>
     * This implementation does not throw ValidationErrors.
     *
     * @param itemData a Map containing raw properties of a DocumentaryUnit
     * @return a new Map containing the objectIdentifier property
     * @throws ValidationError never
     */
    public static Map<String, Object> extractIdentifiers(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = Maps.newHashMap();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_IDENTIFIER));
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
            logger.debug("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    /**
     * Extract a Map containing the properties of a documentary unit's description.
     * Excludes unknown properties, object identifier(s), maintenance events, relations,
     * addresses and *Access relations.
     *
     * @param itemData a Map containing raw properties of a unit
     * @param entity   an EntityClass to get the multi-valuedness of properties for
     * @return a Map representation of a DocumentDescription
     */
    public static Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = Maps.newHashMap();

        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (Map.Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemProperty.getValue());
            } else if (!itemProperty.getKey().startsWith(UNKNOWN_PREFIX)
                    && !itemProperty.getKey().equals(OBJECT_IDENTIFIER)
                    && !itemProperty.getKey().equals(Ontology.IDENTIFIER_KEY)
                    && !itemProperty.getKey().equals(Ontology.OTHER_IDENTIFIERS)
                    && !itemProperty.getKey().startsWith("maintenanceEvent")
                    && !itemProperty.getKey().startsWith("relation")
                    && !itemProperty.getKey().startsWith("IGNORE")
                    && !itemProperty.getKey().startsWith("address/")
                    && !itemProperty.getKey().endsWith("AccessPoint")) {
                description.put(itemProperty.getKey(), flattenNonMultivaluedProperties(
                        itemProperty.getKey(), itemProperty.getValue(), entity));
            }
        }

        return description;
    }

    /**
     * Extract an address node representation from the itemData.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all address/ keys (may be empty)
     */
    public static Map<String, Object> extractAddress(Map<String, Object> itemData) {
        Map<String, Object> address = Maps.newHashMap();
        for (Map.Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().startsWith("address/")) {
                address.put(itemProperty.getKey().substring(8), itemProperty.getValue());
            }
        }
        return address;
    }

    /**
     * Extract datevalues from datamap
     *
     * @param data the data map
     * @return returns a List with the separated datevalues
     */
    static Map<String, String> returnDatesAsString(Map<String, Object> data) {
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

    /**
     * Extract a list of entity bundles for DatePeriods from the data,
     * attempting to parse the unitdate attribute.
     *
     * @param data the data map
     */
    public static List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = Lists.newArrayList();

        for (String key : data.keySet()) {
            if (key.equals("datePeriod") && data.get(key) instanceof List) {
                for (Map<String, Object> event : (List<Map<String, Object>>) data.get(key)) {
                    extractedDates.add(getSubNode(event));
                }
            }
        }

        Map<String, String> dateValues = returnDatesAsString(data);
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
    public static void replaceDates(Map<String, Object> data, List<Map<String, Object>> extractedDates) {
        Map<String, String> dateValues = returnDatesAsString(data);
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
     * Extract the data from a sub-node.
     *
     * @param event a Map of event properties
     * @return a data map
     */
    public static Map<String, Object> getSubNode(Map<String, Object> event) {
        Map<String, Object> me = Maps.newHashMap();
        for (Map.Entry<String, Object> eventEntry : event.entrySet()) {
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

    static String normaliseDate(String date) {
        return normaliseDate(date, false);
    }

    /**
     * Normalise a date in a string.
     *
     * @param date       a String date that needs formatting
     * @param endOfPeriod a string signifying whether this date is the begin of
     *                   a period or the end of a period
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

    public static void overwritePropertyInGraph(Map<String, Object> c, String property, String value) {
        String normValue = normaliseValue(property, value);
        if (normValue != null && !normValue.isEmpty()) {
            logger.debug("overwrite property: {} {}", property, normValue);
            c.put(property, normValue);
        }
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
     * Attempt to extract some date periods. This does not currently put the dates into ISO form.
     *
     * @param date the data map
     * @return returns a Map with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    private static Optional<Map<String, Object>> extractDate(String date) {
        Map<String, Object> data = matchDate(date);
        return data.isEmpty() ? Optional.empty() : Optional.of(data);
    }

    private static Map<String, Object> matchDate(String date) {
        Map<String, Object> data = Maps.newHashMap();
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                logger.debug("matched {}", date);
                data.put(Ontology.DATE_PERIOD_START_DATE, normaliseDate(matcher.group(1)));
                data.put(Ontology.DATE_PERIOD_END_DATE, normaliseDate(matcher.group(matcher
                        .groupCount() > 1 ? 2 : 1), true));
                data.put(Ontology.DATE_HAS_DESCRIPTION, date);
                break;
            }
        }
        return data;
    }

    private static String normaliseValue(String property, String value) {
        String trimmedValue = StringUtils.normalizeSpace(value);
        // Language codes are converted to their 3-letter alternates
        return property.startsWith(LANGUAGE_KEY_PREFIX)
                ? LanguageHelpers.iso639DashTwoCode(trimmedValue)
                : trimmedValue;
    }

    public static List<Map<String, Object>> extractSubNodes(String type, Map<String,Object> data) {

        List<Map<String, Object>> out = Lists.newArrayList();
        Object nodes = data.get(type);
        if (nodes != null && nodes instanceof  List) {
            for (Map<String, Object> event : (List<Map<String, Object>>) nodes) {
                out.add(getSubNode(event));
            }
        }
        return out;
    }



    // Helpers

    private static NodeProperties loadNodeProperties() {
        try (InputStream fis = ImportHelpers.class.getClassLoader().getResourceAsStream(NODE_PROPERTIES);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charsets.UTF_8))) {
            NodeProperties nodeProperties = new NodeProperties();
            String headers = br.readLine();
            nodeProperties.setTitles(headers);

            String line;
            while ((line = br.readLine()) != null) {
                nodeProperties.addRow(line);
            }
            return nodeProperties;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Missing or empty properties file: " + NODE_PROPERTIES);
        }
    }
}
