/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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

package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.impl.CrudViews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Import EAC for a given repository into the database.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class EacImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);

    /**
     * Construct an EacImporter object.
     *
     * @param graph           the framed graph
     * @param permissionScope the permission scope
     * @param log             the import log
     */
    public EacImporter(FramedGraph<?> graph, PermissionScope permissionScope, ImportLog log) {
        super(graph, permissionScope, log);
    }

    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        return importItem(itemData);
    }

    /**
     * @param itemData the item data map
     * @throws ValidationError
     */
    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, permissionScope.idPath());
        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));
        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

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

        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData)) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Ontology.HAS_MAINTENANCE_EVENT, new Bundle(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        for (Map<String, Object> rel : extractRelations(itemData)) {
            if (rel.containsKey("type") && rel.get("type").equals("subjectAccess")) {
                logger.debug("relation found");
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
            }
        }

        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();

//        if (mutation.created()) {
        solveUndeterminedRelationships(frame, descBundle);
//        }

        // There may or may not be a specific scope here...
        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            manager.cast(permissionScope, AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        final String REL = "relation";
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : itemData.keySet()) {
            if (key.equals(REL)) {
                //name identifier
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemData.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (String eventkey : origRelation.keySet()) {
                        if (eventkey.equals("type")) {
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, origRelation.get(eventkey) + "Access");
                        } else if (eventkey.equals("name") && origRelation.get("type").equals("subject")) {
                            Map<String, Object> m = (Map) ((List) origRelation.get(eventkey)).get(0);
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, m.get("concept"));
                            //try to find the original name
                            relationNode.put(Ontology.NAME_KEY, m.get("name"));
                            relationNode.put("cvoc", m.get("cvoc"));
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

    @Override
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = Maps.newHashMap();
        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemData.get(key));
                //resolved in EacHandler
            } else if (key.equals("book")) {
                extractBooks(itemData.get(key), description);
            } else if (!key.startsWith(SaxXmlHandler.UNKNOWN)
                    && !key.equals("objectIdentifier")
                    && !key.equals(Ontology.OTHER_IDENTIFIERS)
                    && !key.equals(Ontology.IDENTIFIER_KEY)
                    && !key.startsWith("maintenanceEvent")
                    && !key.startsWith("IGNORE")
                    && !key.startsWith("relation")
                    && !key.startsWith("address/")) {
                description.put(key, changeForbiddenMultivaluedProperties(key, itemData.get(key), entity));
            }

        }

        return description;
    }

    protected void solveUndeterminedRelationships(HistoricalAgent unit, Bundle descBundle) throws ValidationError {

        //Try to resolve the undetermined relationships
        //we can only create the annotations after the DocumentaryUnit and its Description have been added to the graph,
        //so they have id's. 
        for (Description unitdesc : unit.getDescriptions()) {
            // Put the set of relationships into a HashSet to remove duplicates.
            for (UndeterminedRelationship rel : Sets.newHashSet(unitdesc.getUndeterminedRelationships())) {
                for (String key : rel.asVertex().getPropertyKeys()) {
                    logger.debug("solving undetermindRels: " + key + " " + rel.asVertex().getProperty(key));
                }
                /*
                 * the wp2 undetermined relationship that can be resolved have a 'cvoc' and a 'concept' attribute.
                 * they need to be found in the vocabularies that are in the graph
                 */
                if (rel.asVertex().getPropertyKeys().contains("cvoc")) {
                    String cvoc_id = rel.asVertex().getProperty("cvoc");
                    String concept_id = rel.asVertex().getProperty(LINK_TARGET);
                    logger.debug(cvoc_id + "  " + concept_id);
                    Vocabulary vocabulary;
                    try {
                        vocabulary = manager.getFrame(cvoc_id, Vocabulary.class);
                        for (Concept concept : vocabulary.getConcepts()) {
                            logger.debug("*********************" + concept.getId() + " " + concept.getIdentifier());
                            if (concept.getIdentifier().equals(concept_id)) {
                                try {
                                    Bundle linkBundle = new Bundle(EntityClass.LINK)
                                            .withDataValue(Ontology.LINK_HAS_TYPE, rel.asVertex().getProperty("type").toString())
                                            .withDataValue(Ontology.LINK_HAS_DESCRIPTION, RESOLVED_LINK_DESC);
                                    UserProfile user = manager.cast(log.getActioner(), UserProfile.class);
                                    Link link = new CrudViews<>(framedGraph, Link.class).create(linkBundle, user);
                                    unit.addLink(link);
                                    concept.addLink(link);
                                    link.addLinkBody(rel);
                                } catch (PermissionDenied ex) {
                                    logger.error(ex.getMessage());
                                }

                            }

                        }
                    } catch (ItemNotFound ex) {
                        logger.error("Vocabulary with id " + cvoc_id + " not found. " + ex.getMessage());
                    }

                }
            }
        }
    }

    private void extractBooks(Object books, Map<String, Object> description) {
        List<String> createdBy = Lists.newArrayList();
        List<String> subjectOf = Lists.newArrayList();

        if (books instanceof List) {
            for (Object book : (List) books) {
                if (book instanceof Map) {
                    Map<String, Object> bookentry = (Map) book;
                    boolean created = false;
                    String publication = "";
                    for (String entrykey : bookentry.keySet()) {
                        if (entrykey.equals("bookentry")) {
                            if (bookentry.get(entrykey) instanceof List) {
                                for (Object entry : (List) bookentry.get(entrykey)) {
                                    if (entry instanceof Map) {
                                        if (((Map<String, Object>) entry).containsKey("type") && ((Map<String, Object>) entry).containsKey("bookentry")) {
                                            String type = ((Map<String, Object>) entry).get("type").toString();
                                            String value = ((Map<String, Object>) entry).get("bookentry").toString();
                                            publication = publication.concat(
                                                    (type.equals("isbn") || type.equals("creator")
                                                            ? " " + type + ":"
                                                            : ""
                                                    )
                                                            + value)
                                            ;
                                        }
                                    }
                                }
                            }
                        } else if (entrykey.equals("type")) {
                            created = (bookentry.get(entrykey).equals("creatorOf"));
                        } else {
                            publication = publication.concat(entrykey + ":" + bookentry.get(entrykey));
                        }
                    }
                    if (created) {
                        createdBy.add(publication);
                    } else {
                        subjectOf.add(publication);
                    }

                }
            }
        }
        if (!createdBy.isEmpty()) {
            description.put("createdBy", createdBy);
        }

        if (!subjectOf.isEmpty()) {
            description.put("subjectOf", subjectOf);
        }
    }
}
