package eu.ehri.project.importers.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.ehri.project.importers.util.ImportHelpers.getSubNode;

/**
 * Class for extracting date info from unstructured or semi-structured data and text.
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
public class DateParser {

    private static final Logger logger = LoggerFactory.getLogger(DateParser.class);
    private static final XmlImportProperties dates = new XmlImportProperties("dates.properties");
    private final DateRangeParser rangeParser;

    public DateParser() {
        rangeParser = new DateRangeParser();
    }

    /**
     * Extract a set of dates from input data. The input data is mutated to
     * remove the raw data.
     *
     * @param data a map of input data
     * @return a list of parsed date period maps
     */
    public List<Map<String, Object>> extractDates(Map<String, Object> data) {
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

    private void replaceDates(Map<String, Object> data, List<Map<String, Object>> extractedDates, Map<String, String> dateValues) {
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

    private Optional<Map<String, Object>> extractDate(String date) {
        return rangeParser.tryParse(date).map(DateRange::data);
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
}
