package eu.ehri.project.importers;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    protected final String DESCRIPTION_ID = "descriptionIdentifier";
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
//        Pattern.compile("^(\\d{1,2}-\\d{1,2}-\\d{4}) - (\\d{1,2}-\\d{1,2}-\\d{4})$"),
        Pattern.compile("^\\s*(\\d{4})\\s*-\\s*(\\d{4})"),
        //bundesarchive: 1906/19
        Pattern.compile("^\\s*(\\d{4})/(\\d{2})"),
        Pattern.compile("^\\s*(\\d{4})\\s*/\\s*(\\d{4})"),
//       1935-03/1935-05
        Pattern.compile("^(\\d{4}-\\d{1,2})/(\\d{4}-\\d{1,2})"),
        Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})/(\\d{4}-\\d{1,2}-\\d{1,2})")
    };

    public MapImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    private void extractDateFromValue(List<Map<String, Object>> extractedDates, String value) throws ValidationError {
        logger.debug("date: " + value);
        Map<String, Object> dpb;
        dpb = extractDate(value);
        if (dpb != null) {
            extractedDates.add(dpb);
        }
        logger.debug("nr of dates found: " + extractedDates.size());

    }
    
    /**
     * Extract a list of entity bundles for DatePeriods from the data, attempting to parse the unitdate attribute.
     *
     * @param data
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = new LinkedList<Map<String, Object>>();
        Object value;
        for (Entry<String, Object> property : data.entrySet()) {
            if (dates.containsProperty(property.getKey()) && (value = property.getValue()) != null) {
                logger.debug("---- extract dates -------" + property + ": " + value);
                try {
                    if (property.getValue() instanceof String) {
                        String dateValue = (String) value;
                        for(String d : dateValue.split(",")){
                            extractDateFromValue(extractedDates, d);
                        }
                    } else if (property.getValue() instanceof List) {
                        for (String s : (List<String>) value) {
                            extractDateFromValue(extractedDates, s);
                        }
                    } else {
                        logger.error("ERROR WITH DATES " + value);
                    }


                  
                } catch (ValidationError e) {
                    System.out.println("ERROR WITH DATES");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
        return extractedDates;
    }
    
    /**
     * Extract an Iterable of representations of maintenance events from the itemData.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return
     */
    @Override
    public Iterable<Map<String, Object>> extractMaintenanceEvent(Map<String, Object> itemData)  {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
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
     * Extract a representations of maintenance events from the maintenanceEvent data.
     * 
     * @param event
     * @return 
     */
    @Override
    public Map<String, Object> getMaintenanceEvent(Map<String, Object> event) {
        Map<String, Object> me = new HashMap<String, Object>();
        for (String eventkey : event.keySet()) {
            if (eventkey.equals("maintenanceEvent/type")) {
                me.put(MaintenanceEvent.EVENTTYPE, event.get(eventkey));
            } else if (eventkey.equals("maintenanceEvent/agentType")) {
                me.put(MaintenanceEvent.AGENTTYPE, event.get(eventkey));
            } else {
                me.put(eventkey, event.get(eventkey));
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
     * @param date
     * @return returns a Map with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    private Map<String, Object> extractDate(Object date) /*throws ValidationError*/ {
        logger.debug("date value: " + date);
        Map<String, Object> data = matchDate((String) date);
        return data.isEmpty() ? null : data;
    }

    private Map<String, Object> matchDate(String date) {
        Map<String, Object> data = new HashMap<String, Object>();
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                logger.debug("matched "+ date);
                data.put(Ontology.DATE_PERIOD_START_DATE, normaliseDate(matcher.group(1)));
                data.put(Ontology.DATE_PERIOD_END_DATE, normaliseDate(matcher.group(matcher
                        .groupCount() > 1 ? 2 : 1), Ontology.DATE_PERIOD_END_DATE));
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
     * @param date a String date that needs formatting
     * @param beginOrEnd a string signifying whether this date is the begin of
     * a period or the end of a period
     * @return a String containing the formatted date.
     */
    public static String normaliseDate(String date, String beginOrEnd) {
        DateTimeFormatter fmt = ISODateTimeFormat.date();
        String returndate = fmt.print(DateTime.parse(date));
        if (returndate.startsWith("00")) {
//            logger.debug("strange date: " + returndate);
            returndate = "19" + returndate.substring(2);
            date = "19" + date;
//            logger.debug("strange date: " + returndate);
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
     * @param data
     * @return returns 1 Map of all the tags that were not handled by the property file of this Importer
     */
    protected Iterable<Map<String, Object>> extractOtherProperties(Map<String, Object> data) {
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        Map<String, Object> unit = new HashMap<String, Object>();
        for (Entry<String, Object> property : data.entrySet()) {
            if (property.getKey().startsWith(SaxXmlHandler.UNKNOWN)) {
                unit.put(property.getKey().replace(SaxXmlHandler.UNKNOWN, ""), property.getValue());
            }
        }
        l.add(unit);
        return l;

    }
    
}
