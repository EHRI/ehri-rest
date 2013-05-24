/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
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
public class NiodEuropeanaImporter extends EaImporter{
    private static final Logger logger = LoggerFactory.getLogger(NiodEuropeanaImporter.class);
    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public NiodEuropeanaImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }
    
    @Override
    public DocumentaryUnit importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        return importItem(itemData);
    }
    
    public DocumentaryUnit importItem(Map<String, Object> itemData) throws ValidationError {
        BundleDAO persister = new BundleDAO(framedGraph, permissionScope);
        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractDocumentaryUnit(itemData));
        logger.debug("unit created");

        Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENT_DESCRIPTION));
        logger.debug("description created");
        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle=descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Description.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        unit=unit.withRelation(Description.DESCRIBES, descBundle);

        IdGenerator generator = EntityClass.DOCUMENTARY_UNIT.getIdgen();
        String id = generator.generateId(EntityClass.DOCUMENTARY_UNIT, permissionScope, unit);
        boolean exists = manager.exists(id);
        DocumentaryUnit frame = persister.createOrUpdate(unit.withId(id), DocumentaryUnit.class);
  
        // Set the repository/item relationship
        // Then we need to add a relationship to the repository
        frame.setRepository(framedGraph.frame(permissionScope.asVertex(), Repository.class));
        frame.setPermissionScope(permissionScope);
        
        
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
