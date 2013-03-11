package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAC for a given repository into the database.
 *
 * @author lindar
 *
 */
public class EacImporter extends XmlImporter<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    // An integer that represents how far down the
    // EAD heirarchy tree the current document is.
    public final String DEPTH_ATTR = "depthOfDescription";
    // A (possibly arbitrary) string denoting what the
    // describing body saw fit to name a documentary unit's
    // level of description.
    public final String LEVEL_ATTR = "levelOfDescription";
    // Various date patterns

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param repository
     * @param log
     */
    public EacImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository, ImportLog log) {
        super(framedGraph, repository, log);
    }

    @Override
    public Authority importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        return importItem(itemData);
    }

    /**
     *
     *
     * @param itemData
     * @throws ValidationError
     */
    public Authority importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, repository);
        Bundle unit = new Bundle(EntityClass.AUTHORITY, extractAuthority(itemData));

        Bundle descBundle = new Bundle(EntityClass.AUTHORITY_DESCRIPTION, extractAuthorityDescription(itemData));


        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }

        //add the address to the description bundle
        Map<String, Object> address = extractAddress(itemData);
        if (!address.isEmpty()) {
            descBundle = descBundle.withRelation(AddressableEntity.HAS_ADDRESS, new Bundle(EntityClass.ADDRESS, extractAddress(itemData)));
        }

        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData, itemData.get("objectIdentifier").toString())) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Description.MUTATES, new Bundle(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        unit = unit.withRelation(Description.DESCRIBES, descBundle);

        PermissionScope scope = repository;
        IdGenerator generator = AccessibleEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.AUTHORITY, scope, unit);
        boolean exists = manager.exists(id);
        Authority frame = persister.createOrUpdate(unit.withId(id), Authority.class);

        // Set the repository/item relationship
        frame.setPermissionScope(scope);


        if (exists) {
            for (ImportCallback cb : updateCallbacks) {
                cb.itemImported(frame);
            }
        } else {
            for (ImportCallback cb : createCallbacks) {
                cb.itemImported(frame);
            }
        }
        return frame;

    }

    protected Map<String, Object> extractAuthority(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
//        unit.put(Authority.NAME, itemData.get(Authority.NAME));
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

    protected Map<String, Object> extractAuthorityDescription(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get(key));
            }
            if (!(key.equals(AccessibleEntity.IDENTIFIER_KEY) || key.startsWith("maintenanceEvent") || key.startsWith("address/"))) { //|| key.equals(Authority.NAME)
                unit.put(key, itemData.get(key));
            }
        }
        return unit;
    }
    //TODO: or should this be done in the Handler?
    private static int maintenanceIdentifier = 123;

    @SuppressWarnings("unchecked")
    protected Iterable<Map<String, Object>> extractMaintenanceEvent(Map<String, Object> data, String unitid) throws ValidationError {
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
                    list.add(e2);
                    logger.error("eventType: " + e2.containsKey(MaintenanceEvent.EVENTTYPE) + ", agentType: " + e2.containsKey(MaintenanceEvent.AGENTTYPE));
                }
            }
        }
        logger.error("list size: " + list.size());
        return list;
    }

    protected Map<String, Object> extractAddress(Map<String, Object> data) throws ValidationError {
        Map<String, Object> address = new HashMap<String, Object>();
        for (String key : data.keySet()) {
            //ADDRESS_NAME
            if (key.startsWith("address/")) {
                address.put(key.substring(8), data.get(key));
            }
        }
        if (!address.isEmpty() && !address.containsKey(Address.ADDRESS_NAME)) {
            address.put(Address.ADDRESS_NAME, address.get("street") + " " + address.get("municipality") + " " + address.get("country"));
        }
        return address;
    }
}
