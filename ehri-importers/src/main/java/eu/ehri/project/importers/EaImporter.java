package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.NamedEntity;
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
        Map<String, Object> unit = extractDocumentaryUnit(itemData);
        unit.put("typeOfEntity", itemData.get("typeOfEntity"));
        return unit;
    }

     protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
//        unit.put(NamedEntity.NAME, itemData.get(NamedEntity.NAME));
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

    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get(key));
            }else if ( !key.startsWith(SaxXmlHandler.UNKNOWN) 
                    && ! key.equals("objectIdentifier") 
                    && ! key.equals(AccessibleEntity.IDENTIFIER_KEY) 
                    && ! key.startsWith("maintenanceEvent") 
                    && ! key.startsWith("address/")) {
               description.put(key, changeForbiddenMultivaluedProperties(key, itemData.get(key), entity));
            }
        }
//        assert(description.containsKey(AccessibleEntity.IDENTIFIER_KEY));  //not required anymore
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
                        if (eventkey.equals("maintenanceEvent/type")) {
                            e2.put(MaintenanceEvent.EVENTTYPE, event.get(eventkey));
                        } else if (eventkey.equals("maintenanceEvent/agentType")) {
                            e2.put(MaintenanceEvent.AGENTTYPE, event.get(eventkey));
                        } else {
                            e2.put(eventkey, event.get(eventkey));
                        }
                    }
                    if (!e2.containsKey(AccessibleEntity.IDENTIFIER_KEY)) {
                        if (e2.containsKey("maintenanceEvent/date")) {
                            e2.put(AccessibleEntity.IDENTIFIER_KEY, unitid + ":" + e2.get("maintenanceEvent/date"));
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
        return address;
    }
}
