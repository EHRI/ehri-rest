package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.ehri.project.models.base.PermissionScope;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lindar
 *
 */
public abstract class XmlImporter<T> extends AbstractImporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(XmlImporter.class);
    protected final String OBJECT_ID = "objectIdentifier";
    protected final String DESCRIPTION_ID = "descriptionIdentifier";
    private PropertiesConfig dates = new PropertiesConfig("dates.properties");
    // Various date patterns
    private Pattern[] datePatterns = {
        // Yad Vashem, ICA-Atom style: 1924-1-1 - 1947-12-31
        Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})\\s?-\\s?(\\d{4}-\\d{1,2}-\\d{1,2})$"),
        Pattern.compile("^(\\d{4})\\s?-\\s?(\\d{4})$"),
        Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
        Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
        Pattern.compile("^(\\d{4}s)-\\[(\\d{4}s)\\]$"),
        Pattern.compile("^\\[(\\d{4})\\]$"), Pattern.compile("^(\\d{4})$"),
        Pattern.compile("^(\\d{2})th century$"),
        Pattern.compile("^\\s*(\\d{4})\\s*-\\s*(\\d{4})")
    };

    public XmlImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    /**
     * Extract a list of entity bundles for DatePeriods from the data, attempting to parse the unitdate attribute.
     *
     * @param data
     */
    public List<Map<String, Object>> extractDates(Map<String, Object> data) {
        List<Map<String, Object>> extractedDates = new LinkedList<Map<String, Object>>();
        Object value;
        for (String key : data.keySet()) {
            if (dates.containsProperty(key) && (value = data.get(key)) != null) {
                try {
                    Map<String, Object> dpb = extractDate(value);
                    if (dpb != null) {
                        extractedDates.add(dpb);
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
     * Attempt to extract some date periods. This does not currently put the dates into ISO form.
     *
     * @param date
     * @return returns a Map with DatePeriod.START_DATE and DatePeriod.END_DATE values
     * @throws ValidationError
     */
    private Map<String, Object> extractDate(Object date) throws ValidationError {
        logger.debug("date value: " + date);
        Map<String, Object> data = new HashMap<String, Object>();
        if (date instanceof String) {
            data = matchDate((String) date);
        } else if (date instanceof List) {
            for (String s : (List<String>) date) {
                data.putAll(data);
            }
        } else {
            logger.error("ERROR WITH DATES " + date);
        }
        return data.isEmpty() ? null : data;
    }

    private Map<String, Object> matchDate(String date) {
        Map<String, Object> data = new HashMap<String, Object>();
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                data.put(DatePeriod.START_DATE, normaliseDate(matcher.group(1)));
                data.put(DatePeriod.END_DATE, normaliseDate(matcher.group(matcher
                        .groupCount() > 1 ? 2 : 1)));
                break;
            }
        }
        return data;
    }

    private String normaliseDate(String date) {
        DateTimeFormatter fmt = ISODateTimeFormat.date();
        return fmt.print(DateTime.parse(date));
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
        for (String key : data.keySet()) {
            if (key.startsWith(SaxXmlHandler.UNKNOWN)) {
                unit.put(key.replace(SaxXmlHandler.UNKNOWN, ""), data.get(key));
            }
        }
        l.add(unit);
        return l;

    }
    
}
