package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEvent;
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
    protected static final String LINK_TARGET = "target";


    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EaImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    /**
     * Extract properties from the itemData Map that belong to a generic unit and
     * returns them as a new Map. Calls extractDocumentaryUnit.
     * 
     * @param itemData a Map representation of a unit
     * @return a new Map containing those properties that are specific to a unit
     * @throws ValidationError when extractDocumentaryUnit throws it
     */
    protected Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = extractDocumentaryUnit(itemData);
        unit.put("typeOfEntity", itemData.get("typeOfEntity"));
        return unit;
    }

    /**
     * Extract DocumentaryUnit properties from the itemData and return them as a new Map.
     * This implementation only extracts the objectIdentifier.
     * 
     * This implementation does not throw ValidationErrors.
     * 
     * @param itemData a Map containing raw properties of a DocumentaryUnit
     * @return a new Map containing the objectIdentifier property
     * @throws ValidationError never
     */
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
        return unit;
    }
     
    /**
     * Utility method to return an Iterable<T> as a List<T>.
     * 
     * @param iter an Iterable of type T
     * @return the input as a List of the same type T
     */
    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext()) {
            lst.add(it.next());
        }
        return lst;
    }
    
    /**
     * Extract properties from the itemData Map that are marked as unknown, and return them in a new Map.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError 
     */
    protected Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.startsWith(SaxXmlHandler.UNKNOWN)) {
                unknowns.put(key.substring(SaxXmlHandler.UNKNOWN.length()), itemData.get(key));
            }
        }
        return unknowns;
    }
    
    /**
     * Extract node representations for related nodes based on the 'relation' property in the supplied data Map.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return an Iterable of new Maps representing related nodes and their types
     */
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        final String REL = "relation";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : itemData.keySet()) {
            if (key.equals(REL)) {
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemData.get(key)) {
                    Map<String, Object> relationNode = new HashMap<String, Object>();
                    for (String eventkey : origRelation.keySet()) {
                        if (eventkey.equals(REL + "/type")) {
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, origRelation.get(eventkey));
                        } else if (eventkey.equals(REL + "/url")) {
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, origRelation.get(eventkey));
                        } else if (eventkey.equals(REL + "/" + Ontology.NAME_KEY)) {
                            //try to find the original identifier
                            relationNode.put(Ontology.NAME_KEY, origRelation.get(eventkey));
                        } else if (eventkey.equals(REL + "/notes")) {
                            relationNode.put(Ontology.LINK_HAS_DESCRIPTION, origRelation.get(eventkey));
                        } else {
                            relationNode.put(eventkey, origRelation.get(eventkey));
                        }
                    }
                    if (!relationNode.containsKey(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)) {
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccess");
                    }
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    /**
     * Extract a Map containing the properties of a documentary unit's description.
     * Excludes unknown properties, object identifier(s), maintenance events, relations,
     * addresses and *Access relations.
     * 
     * @param itemData a Map containing raw properties of a unit 
     * @param entity
     * @return
     */
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemData.get(key));
            }else if ( !key.startsWith(SaxXmlHandler.UNKNOWN) 
                    && ! key.equals("objectIdentifier") 
                    && ! key.equals(Ontology.IDENTIFIER_KEY)
                    && ! key.equals(Ontology.OTHER_IDENTIFIERS)
                    && ! key.startsWith("maintenanceEvent") 
                    && ! key.startsWith("relation")
                    && ! key.startsWith("address/")
                    && ! key.endsWith("Access")) {
               description.put(key, changeForbiddenMultivaluedProperties(key, itemData.get(key), entity));
            }
        }
//        assert(description.containsKey(IdentifiableEntity.IDENTIFIER_KEY));
        return description;
    }
    
    /**
     * Extract an Iterable of representations of maintenance events from the itemData.
     * 
     * TODO the unitid parameter is not used in the code.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @param unitid
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Iterable<Map<String, Object>> extractMaintenanceEvent(Map<String, Object> itemData, String unitid)  {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : itemData.keySet()) {
            if (key.equals("maintenanceEvent")) {
                for (Map<String, Object> event : (List<Map<String, Object>>) itemData.get(key)) {
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
     * Extract an address node representation from the itemData.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all address/ keys (may be empty)
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
