/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class EagImporter extends EaImporter{
    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    
    /**
     * Construct an EagImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EagImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public Repository importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        return importItem(itemData);
    }

    /**
     *
     *
     * @param itemData
     * @throws ValidationError
     */
    public Repository importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, permissionScope);
        Bundle unit = new Bundle(EntityClass.REPOSITORY, extractUnit(itemData));

        Map<String, Object> descmap = extractUnitDescription(itemData);
        descmap.put(IdentifiableEntity.IDENTIFIER_KEY, descmap.get(IdentifiableEntity.IDENTIFIER_KEY)+"#desc");
        Bundle descBundle = new Bundle(EntityClass.REPOSITORY_DESCRIPTION, descmap);


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

        IdGenerator generator = IdentifiableEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.REPOSITORY, SystemScope.getInstance(), unit);
        boolean exists = manager.exists(id);
        Repository frame = persister.createOrUpdate(unit.withId(id), Repository.class);

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
}
