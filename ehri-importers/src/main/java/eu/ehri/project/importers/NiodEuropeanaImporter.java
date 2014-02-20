/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;

import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.Mutation;
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
    public DocumentaryUnit importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        return importItem(itemData);
    }
    
    public DocumentaryUnit importItem(Map<String, Object> itemData) throws ValidationError {
        BundleDAO persister = getPersister();

        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractDocumentaryUnit(itemData));
        logger.debug("unit created");

        Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENT_DESCRIPTION));
        logger.debug("description created");
        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle=descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        unit=unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<DocumentaryUnit> mutation = persister.createOrUpdate(unit, DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        // Set the repository/item relationship
        // Then we need to add a relationship to the repository
        if (mutation.created()) {
            frame.setRepository(framedGraph.frame(permissionScope.asVertex(), Repository.class));
            frame.setPermissionScope(permissionScope);
        }
        handleCallbacks(mutation);
        return frame;
    }
}
