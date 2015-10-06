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

package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer of Map based representations of documentary units, historical agents,
 * virtual collections and other entities. Does not implement the actual
 * import methods.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class MapImporter extends AbstractImporter<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(MapImporter.class);
    protected final String OBJECT_ID = "objectIdentifier";
    private final XmlImportProperties dates = new XmlImportProperties("dates.properties");

    // Various date patterns
    private final Pattern[] datePatterns = {
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

    public MapImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    private void extractDateFromValue(List<Map<String, Object>> extractedDates, String value) throws ValidationError {
        logger.debug("date: {}", value);
        Map<String, Object> dpb;
        dpb = extractDate(value);
        if (dpb != null) {
            extractedDates.add(dpb);
        }
        logger.debug("nr of dates found: {}", extractedDates.size());
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
     * Extract a list of entity bundles for DatePeriods from the data, attempting to parse the unitdate attribute.
     *
     * @param data the data map
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = Lists.newLinkedList();
        Map<String, String> datevalues = returnDatesAsString(data, dates);
        for (String s : datevalues.keySet()) {
            try {
                extractDateFromValue(extractedDates, s);
            } catch (ValidationError e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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
        Map<String, String> datevalues = returnDatesAsString(data, dates);
        Map<String, String> datetypes = Maps.newHashMap();
        for (String datevalue : datevalues.keySet()) {
            datetypes.put(datevalues.get(datevalue), null);
        }


        for (Map<String, Object> datemap : extractedDates) {
            datevalues.remove(datemap.get(Ontology.DATE_HAS_DESCRIPTION));
        }
        //replace dates in data map
        for (String datevalue : datevalues.keySet()) {
            String datetype = datevalues.get(datevalue);
            logger.debug("{} -- {}", datevalue, datetype);
            if (datetypes.containsKey(datetype) && datetypes.get(datetype) != null) {
                datetypes.put(datetype, datetypes.get(datetype) + ", " + datevalue.trim());
            } else {
                datetypes.put(datetype, datevalue.trim());
            }
        }
        for (String datetype : datetypes.keySet()) {
            logger.debug("datetype {} -- {}", datetype, datetypes.get(datetype));
            if (datetypes.get(datetype) == null) {
                data.remove(datetype);
            } else {
                data.put(datetype, datetypes.get(datetype));
            }
            logger.debug("" + data.get(datetype));
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
            if (key.equals("maintenanceEvent")) {
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
            if (eventEntry.getKey().equals("maintenanceEvent/type")) {
                me.put(MaintenanceEvent.EVENTTYPE, eventEntry.getValue());
            } else if (eventEntry.getKey().equals("maintenanceEvent/agentType")) {
                me.put(MaintenanceEvent.AGENTTYPE, eventEntry.getValue());
            } else {
                me.put(eventEntry.getKey(), eventEntry.getValue());
            }
        }
        if (!me.containsKey(MaintenanceEvent.EVENTTYPE)) {
            me.put(MaintenanceEvent.EVENTTYPE, "unknown event type");
        }
        return me;
    }


    @Override
    public MaintenanceEvent importMaintenanceEvent(Map<String, Object> event) {
        BundleDAO persister = new BundleDAO(framedGraph, permissionScope.idPath());
        try {
            Bundle unit = new Bundle(EntityClass.MAINTENANCE_EVENT, event);
            //only if some source is given (especially with a creation) should a ME be created
            for (String e : unit.getPropertyKeys()) {
                logger.debug(e);
            }
            if (unit.getDataValue("source") != null) {
                Mutation<MaintenanceEvent> mutation = persister.createOrUpdate(unit, MaintenanceEvent.class);
                return mutation.getNode();
            }
        } catch (ValidationError ex) {
            logger.error(ex.getMessage());
        }
        return null;
    }

    /**
     * Attempt to extract some date periods. This does not currently put the dates into ISO form.
     *
     * @param date the data map
     * @return returns a Map with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    private Map<String, Object> extractDate(Object date) /*throws ValidationError*/ {
        logger.debug("date value: {}", date);
        Map<String, Object> data = matchDate((String) date);
        return data.isEmpty() ? null : data;
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

    private static String normaliseDate(String date) {
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
    public static String normaliseDate(String date, String beginOrEnd) {
        DateTimeFormatter fmt = ISODateTimeFormat.date();
        String returndate = fmt.print(DateTime.parse(date));
        if (returndate.startsWith("00")) {
            returndate = "19" + returndate.substring(2);
            date = "19" + date;
        }
        if (Ontology.DATE_PERIOD_END_DATE.equals(beginOrEnd)) {
            if (!date.equals(returndate)) {
                ParsePosition p = new ParsePosition(0);
                new SimpleDateFormat("yyyy-MM").parse(date, p);
                if (p.getIndex() > 0) {
                    returndate = fmt.print(DateTime.parse(date).plusMonths(1).minusDays(1));
                } else {
                    p = new ParsePosition(0);
                    new SimpleDateFormat("yyyy").parse(date, p);
                    if (p.getIndex() > 0) {
                        returndate = fmt.print(DateTime.parse(date).plusYears(1).minusDays(1));
                    }
                }
            }
        }
        return returndate;
    }

    //TODO: for now, it only returns 1 unknown node object, but it could be more accurate to return several

    /**
     * extract data nodes from the data, that are not covered by their respectable properties file.
     *
     * @param data the data map
     * @return returns 1 Map of all the tags that were not handled by the property file of this Importer
     */
    protected Iterable<Map<String, Object>> extractOtherProperties(Map<String, Object> data) {
        List<Map<String, Object>> l = Lists.newArrayList();
        Map<String, Object> unit = Maps.newHashMap();
        for (Entry<String, Object> property : data.entrySet()) {
            if (property.getKey().startsWith(SaxXmlHandler.UNKNOWN)) {
                unit.put(property.getKey().replace(SaxXmlHandler.UNKNOWN, ""), property.getValue());
            }
        }
        l.add(unit);
        return l;

    }
}
