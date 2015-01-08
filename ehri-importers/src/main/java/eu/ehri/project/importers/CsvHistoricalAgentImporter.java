/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;

import java.util.Map;

import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;

/**
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class CsvHistoricalAgentImporter extends CsvAuthoritativeItemImporter {

    public CsvHistoricalAgentImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = getPersister();

        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));

        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();

        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            manager.cast(permissionScope, AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

}
