package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
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

/**
 * Import EAD for a given repository into the database. Due to the laxness of
 * the EAD standard this is a fairly complex procedure. An EAD a single entity
 * at the highest level of description or multiple top-level entities, with or
 * without a hierarchical structure describing their child items. This means
 * that we need to recursively descend through the archdesc and c01-12 levels.
 *
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author michaelb
 *
 */
public class EacImporter extends XmlImporter<Map<String, Object>> {

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
     * @param topLevelEad
     */
    public EacImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            ImportLog log) {
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
            descBundle=descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        unit=unit.withRelation(Description.DESCRIBES, descBundle);
//        EntityBundle<AuthorityDescription> adesc = 
//                new BundleFactory<AuthorityDescription>().buildBundle(extractAuthorityDescription(itemData), AuthorityDescription.class);
//        unit.addRelation(Description.DESCRIBES,adesc);
        
//        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData)) {
//            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
//            EntityBundle<MaintenanceEvent> event = new BundleFactory<MaintenanceEvent>().buildBundle(dpb,
//                            MaintenanceEvent.class);
//            adesc.addRelation(Description.MUTATES, event);
//        }
                
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
            if(key.equals("descriptionIdentifier"))
                unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get(key));
            if (!(key.equals(AccessibleEntity.IDENTIFIER_KEY)  || key.startsWith("maintenanceEvent") )) { //|| key.equals(Authority.NAME)
                unit.put(key, itemData.get(key));
            }
        }
        return unit;
    }
    
   
}
