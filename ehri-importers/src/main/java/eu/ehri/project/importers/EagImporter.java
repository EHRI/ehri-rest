/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class EagImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    private final Pattern priorityPattern = Pattern.compile("Priority: (-?\\d+)");
    public static final String MAINTENANCE_NOTES = "maintenanceNotes";
    public static final String PRIORITY = "priority";

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
    public Repository importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        return importItem(itemData);
    }

    @Override
    public Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> data = super.extractUnit(itemData);

        // MB: Hack hack hack - extract EHRI-specific 'priority' field out of the
        // pattern "Priority: <digit>" in the maintenanceNotes field.
        Object notes = itemData.get(MAINTENANCE_NOTES);
        if (notes != null) {
            if (notes instanceof ArrayList<?>) {
                for (Object n : (ArrayList<?>) notes) {
                    if (n instanceof String) {
                        Matcher m = priorityPattern.matcher((String) n);
                        if (m.find()) {
                            data.put(PRIORITY, Integer.parseInt(m.group(1)));
                        }
                    }
                }
            }
        }

        return data;
    }

    /**
     *
     *
     * @param itemData
     * @throws ValidationError
     */
    @Override
    public Repository importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, permissionScope.idPath());

        Bundle unit = new Bundle(EntityClass.REPOSITORY, extractUnit(itemData));

        Map<String, Object> descmap = extractUnitDescription(itemData, EntityClass.REPOSITORY_DESCRIPTION);
        descmap.put(Ontology.IDENTIFIER_KEY, descmap.get(Ontology.IDENTIFIER_KEY) + "#desc");
        Bundle descBundle = new Bundle(EntityClass.REPOSITORY_DESCRIPTION, descmap);

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }

        //add the address to the description bundle
        Map<String, Object> address = extractAddress(itemData);
        if (!address.isEmpty()) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_ADDRESS, new Bundle(EntityClass.ADDRESS, address));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData, itemData.get("objectIdentifier").toString())) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Ontology.HAS_MAINTENANCE_EVENT, new Bundle(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<Repository> mutation = persister.createOrUpdate(unit, Repository.class);
        handleCallbacks(mutation);

        if (mutation.created()) {
            mutation.getNode().setCountry(framedGraph.frame(permissionScope.asVertex(), Country.class));
            mutation.getNode().setPermissionScope(permissionScope);
        }
        return mutation.getNode();
    }
}
