/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.eac;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.importers.links.LinkResolver;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Import EAC for a given repository into the database.
 */
public class EacImporter extends AbstractImporter<Map<String, Object>, HistoricalAgent> {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    private static final String REL_TYPE = "type";
    private static final String REL_NAME = "name";

    private final LinkResolver linkResolver;

    /**
     * Construct an EacImporter object.
     *
     * @param graph           the framed graph
     * @param permissionScope the permission scope
     * @param actioner        the current user
     * @param options         the import options
     * @param log             the import log
     */
    public EacImporter(FramedGraph<?> graph, PermissionScopeFinder permissionScope, Actioner actioner, ImportOptions options, ImportLog log) {
        super(graph, permissionScope, actioner, options, log);
        linkResolver = new LinkResolver(graph, actioner.as(Accessor.class));
    }

    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        return importItem(itemData);
    }

    /**
     * @param itemData the item data map
     * @return the new historical agent
     * @throws ValidationError if data constraints are not met
     */
    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData) throws ValidationError {

        Bundle descBundle = Bundle.of(EntityClass.HISTORICAL_AGENT_DESCRIPTION,
                extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        // Add dates and descriptions to the bundle since they are @Dependent relations.
        for (Map<String, Object> dpb : ImportHelpers.extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, Bundle.of(EntityClass.DATE_PERIOD, dpb));
        }

        // add the address to the description bundle
        Map<String, Object> address = ImportHelpers.extractAddress(itemData);
        if (!address.isEmpty()) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_ADDRESS, Bundle.of(EntityClass.ADDRESS, address));
        }

        Map<String, Object> unknowns = ImportHelpers.extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.trace("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, Bundle.of(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }

        for (Map<String, Object> dpb : ImportHelpers.extractSubNodes(Entities.MAINTENANCE_EVENT, itemData)) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Ontology.HAS_MAINTENANCE_EVENT, Bundle.of(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        for (Map<String, Object> rel : extractRelations(itemData)) {
            if (rel.containsKey(REL_TYPE) && rel.get(REL_TYPE).equals(AccessPointType.subject.name())) {
                logger.trace("relation found: {}", rel.get(REL_TYPE));
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT,
                        Bundle.of(EntityClass.ACCESS_POINT, rel));
            }
        }

        Bundle unit = Bundle.of(EntityClass.HISTORICAL_AGENT, extractUnit(itemData))
                .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        final String localId = getLocalIdentifier(unit);
        final PermissionScope permissionScope = scopeFinder.apply(localId);
        BundleManager bundleManager = new BundleManager(framedGraph, permissionScope.idPath());

        Mutation<HistoricalAgent> mutation = bundleManager.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();
        linkResolver.solveUndeterminedRelationships(frame);

        // There may or may not be a specific scope here...
        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            permissionScope.as(AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    private Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : itemData.keySet()) {
            if (key.equals(Entities.ACCESS_POINT)) {
                //name identifier
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemData.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (String eventkey : origRelation.keySet()) {
                        if (eventkey.equals(REL_TYPE)) {
                            relationNode.put(Ontology.ACCESS_POINT_TYPE, origRelation.get(eventkey));
                        } else if (eventkey.equals(REL_NAME)) {
                            //try to find the original identifier
                            relationNode.put(ImportHelpers.LINK_TARGET, origRelation.get("concept"));
                            //try to find the original name
                            relationNode.put(Ontology.NAME_KEY, origRelation.get(REL_NAME));
                            relationNode.put("cvoc", origRelation.get("cvoc"));
                        } else {
                            relationNode.put(eventkey, origRelation.get(eventkey));
                        }
                    }
                    if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                        // Corporate bodies are the default type
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, AccessPointType.corporateBody);
                    }
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = Maps.newHashMap();
        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemData.get(key));
            } else if (!key.startsWith(ImportHelpers.UNKNOWN_PREFIX)
                    && !key.equals(ImportHelpers.OBJECT_IDENTIFIER)
                    && !key.equals(Ontology.OTHER_IDENTIFIERS)
                    && !key.equals(Ontology.IDENTIFIER_KEY)
                    && !key.startsWith(Entities.MAINTENANCE_EVENT)
                    && !key.startsWith(Entities.ACCESS_POINT)
                    && !key.startsWith("IGNORE")
                    && !key.startsWith("address/")) {
                description.put(key, ImportHelpers.flattenNonMultivaluedProperties(key, itemData.get(key), entity));
            }
        }

        return description;
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> data = ImportHelpers.extractIdentifiers(itemData);
        data.put("typeOfEntity", itemData.get("typeOfEntity"));
        return data;
    }
}
