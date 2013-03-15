package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.AccessibleEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAC for a given repository into the database.
 *
 * @author lindar
 *
 */
public abstract class EaImporter extends XmlImporter<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(EaImporter.class);
    // An integer that represents how far down the
    // EAD heirarchy tree the current document is.
    public final String DEPTH_ATTR = "depthOfDescription";
    // A (possibly arbitrary) string denoting what the
    // describing body saw fit to name a documentary unit's
    // level of description.
    public final String LEVEL_ATTR = "levelOfDescription";
    // Various date patterns

    public static final String ADDRESS_NAME = "name";

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EaImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    protected Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
//        unit.put(HistoricalAgent.NAME, itemData.get(HistoricalAgent.NAME));
        unit.put("typeOfEntity", itemData.get("typeOfEntity"));
        return unit;
    }

    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext()) {
            lst.add(it.next());
        }
        return lst;
    }
    
    /**
     * 
     * @param itemData
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError 
     */
    protected Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.startsWith(SaxXmlHandler.UNKNOWN)) {
                unknowns.put(key, itemData.get(key));
            }
        }
        return unknowns;
    }

    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData) {
        Map<String, Object> description = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get(key));
            }else if ( !key.startsWith(SaxXmlHandler.UNKNOWN) 
                    && ! key.equals("objectIdentifier") 
                    && ! key.equals(AccessibleEntity.IDENTIFIER_KEY) 
                    && ! key.startsWith("maintenanceEvent") 
                    && ! key.startsWith("address/")) { 
                description.put(key, itemData.get(key));
            }
        }
        assert(description.containsKey(AccessibleEntity.IDENTIFIER_KEY));
        return description;
    }
    //TODO: or should this be done in the Handler?
    private static int maintenanceIdentifier = 123;

    @SuppressWarnings("unchecked")
    protected Iterable<Map<String, Object>> extractMaintenanceEvent(Map<String, Object> data, String unitid)  {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : data.keySet()) {
            if (key.equals("maintenanceEvent")) {
                for (Map<String, Object> event : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> e2 = new HashMap<String, Object>();
                    for (String eventkey : event.keySet()) {
                        if (eventkey.equals("maintenanceEventType")) {
                            e2.put(MaintenanceEvent.EVENTTYPE, event.get(eventkey));
                        } else if (eventkey.equals("maintenanceEventAgentType")) {
                            e2.put(MaintenanceEvent.AGENTTYPE, event.get(eventkey));
                        } else {
                            e2.put(eventkey, event.get(eventkey));
                        }
                    }
                    if (!e2.containsKey(AccessibleEntity.IDENTIFIER_KEY)) {
                        if (e2.containsKey("maintenanceEventDate")) {
                            e2.put(AccessibleEntity.IDENTIFIER_KEY, unitid + ":" + e2.get("maintenanceEventDate"));
                        } else {
                            e2.put(AccessibleEntity.IDENTIFIER_KEY, maintenanceIdentifier++);
                        }
                    }
                    if (!e2.containsKey(MaintenanceEvent.EVENTTYPE)){
                        e2.put(MaintenanceEvent.EVENTTYPE, "unknown event type");
                    }
                    list.add(e2);
                }
            }
        }
        return list;
    }

    /**
     * 
     * @param itemData
     * @return returns a Map with all address/ keys
     * @throws ValidationError 
     */
    protected Map<String, Object> extractAddress(Map<String, Object> itemData)  {
        Map<String, Object> address = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.startsWith("address/")) {
                address.put(key.substring(8), itemData.get(key));
            }
        }
        if (!address.isEmpty() && !address.containsKey(ADDRESS_NAME)) {
            address.put(ADDRESS_NAME, address.get("street") + " " + address.get("municipality") + " " + address.get("country"));
        }
        return address;
    }
}
