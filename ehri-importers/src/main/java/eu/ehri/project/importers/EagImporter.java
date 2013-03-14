/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
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
     * @param repository
     * @param log
     */
    public EagImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository, ImportLog log) {
        super(framedGraph, repository, log);
    }

    @Override
    public Agent importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        return importItem(itemData);
    }

    /**
     *
     *
     * @param itemData
     * @throws ValidationError
     */
    public Agent importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, permissionScope);
        Bundle unit = new Bundle(EntityClass.AGENT, extractUnit(itemData));

        Map<String, Object> descmap = extractUnitDescription(itemData);
        descmap.put(AccessibleEntity.IDENTIFIER_KEY, descmap.get(AccessibleEntity.IDENTIFIER_KEY)+"#desc");
        Bundle descBundle = new Bundle(EntityClass.AGENT_DESCRIPTION, descmap);


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

        IdGenerator generator = AccessibleEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.AGENT, SystemScope.getInstance(), unit);
        boolean exists = manager.exists(id);
        Agent frame = persister.createOrUpdate(unit.withId(id), Agent.class);

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
