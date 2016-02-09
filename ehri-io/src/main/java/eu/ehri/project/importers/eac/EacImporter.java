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

package eu.ehri.project.importers.eac;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.EaImporter;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.AccessPoint;
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
 */
public class EacImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    public static final String REL_TYPE = "type";
    public static final String REL_NAME = "name";

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
        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION,
                extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }

        // add the address to the description bundle
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
            if (rel.containsKey(REL_TYPE) && rel.get(REL_TYPE).equals("subjectAccess")) {
                logger.debug("relation found");
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.ACCESS_POINT, rel));
            }
        }

        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData))
                .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();
        solveUndeterminedRelationships(frame, descBundle);

        // There may or may not be a specific scope here...
        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            permissionScope.as(AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        String relKey = "relation";
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : itemData.keySet()) {
            if (key.equals(relKey)) {
                //name identifier
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemData.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (String eventkey : origRelation.keySet()) {
                        if (eventkey.equals(REL_TYPE)) {
                            relationNode.put(Ontology.ACCESS_POINT_TYPE, origRelation.get(eventkey) + "Access");
                        } else if (eventkey.equals(REL_NAME) && origRelation.get(REL_TYPE).equals("subject")) {
                            Map<String, Object> m = (Map) ((List) origRelation.get(eventkey)).get(0);
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, m.get("concept"));
                            //try to find the original name
                            relationNode.put(Ontology.NAME_KEY, m.get(REL_NAME));
                            relationNode.put("cvoc", m.get("cvoc"));
                        } else {
                            relationNode.put(eventkey, origRelation.get(eventkey));
                        }
                    }
                    if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                        // Corporate bodies are the default type
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, "corporateBodyAccess");
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
                description.put(key, flattenNonMultivaluedProperties(key, itemData.get(key), entity));
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
            for (AccessPoint rel : Sets.newHashSet(unitdesc.getAccessPoints())) {
                Vertex relationVertex = rel.asVertex();
                for (String key : relationVertex.getPropertyKeys()) {
                    logger.debug("solving undetermindRels: {} {} ({})",
                            key, relationVertex.getProperty(key), descBundle.getId());
                }
                /*
                 * the wp2 undetermined relationship that can be resolved have a 'cvoc' and a 'concept' attribute.
                 * they need to be found in the vocabularies that are in the graph
                 */
                if (relationVertex.getPropertyKeys().contains("cvoc")) {
                    String cvocId = relationVertex.getProperty("cvoc");
                    String conceptId = relationVertex.getProperty(LINK_TARGET);
                    logger.debug("{} -> {}", cvocId, conceptId);
                    try {
                        Vocabulary vocabulary = manager.getEntity(cvocId, Vocabulary.class);
                        for (Concept concept : vocabulary.getConcepts()) {
                            logger.debug("********************* {} {}", concept.getId(), concept.getIdentifier());
                            if (concept.getIdentifier().equals(conceptId)) {
                                try {
                                    String linkType = relationVertex.getProperty(REL_TYPE);
                                    Bundle linkBundle = new Bundle(EntityClass.LINK)
                                            .withDataValue(Ontology.LINK_HAS_TYPE, linkType)
                                            .withDataValue(Ontology.LINK_HAS_DESCRIPTION, RESOLVED_LINK_DESC);
                                    UserProfile user = log.getActioner().as(UserProfile.class);
                                    Link link = new CrudViews<>(framedGraph, Link.class).create(linkBundle, user);
                                    unit.addLink(link);
                                    concept.addLink(link);
                                    link.addLinkBody(rel);
                                } catch (PermissionDenied ex) {
                                    logger.error("Unexpected permission error on EAC relationship creation: {}", ex);
                                    throw new RuntimeException(ex);
                                }
                            }
                        }
                    } catch (ItemNotFound ex) {
                        logger.error("Vocabulary with id {} not found: {}", cvocId, ex);
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
                    Map bookEntry = (Map) book;
                    boolean created = false;
                    String publication = "";
                    for (Object entryKey : bookEntry.keySet()) {
                        switch ((String) entryKey) {
                            case "bookentry":
                                if (bookEntry.get(entryKey) instanceof List) {
                                    for (Object entry : (List) bookEntry.get(entryKey)) {
                                        if (entry instanceof Map
                                                && ((Map) entry).containsKey(REL_TYPE)
                                                && ((Map) entry).containsKey("bookentry")) {
                                            String type = ((Map) entry).get(REL_TYPE).toString();
                                            String value = ((Map) entry).get("bookentry").toString();
                                            String join = type.equals("isbn") || type.equals("creator")
                                                    ? " " + type + ":"
                                                    : "";
                                            publication = publication.concat(join + value);
                                        }
                                    }
                                }
                                break;
                            case REL_TYPE:
                                created = (bookEntry.get(entryKey).equals("creatorOf"));
                                break;
                            default:
                                publication = publication.concat(entryKey + ":" + bookEntry.get(entryKey));
                                break;
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
