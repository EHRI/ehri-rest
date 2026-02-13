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

package eu.ehri.project.importers.ead;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.importers.links.LinkResolver;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiPredicate;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 * <p>
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 */
public class EadImporter extends AbstractImporter<Map<String, Object>, AbstractUnit> {

    private static final Logger logger = LoggerFactory.getLogger(EadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private final EntityClass unitEntity = EntityClass.DOCUMENTARY_UNIT;
    private final Serializer mergeSerializer;
    private final LinkResolver linkResolver;

    public static final String ACCESS_POINT = "AccessPoint";

    /**
     * Construct an EadImporter object.
     *
     * @param graph       the framed graph
     * @param actioner    the current user
     * @param scopeFinder the permission scope
     * @param options     the import options
     * @param log         the log
     */
    public EadImporter(FramedGraph<?> graph, PermissionScopeFinder scopeFinder, Actioner actioner, ImportOptions options, ImportLog log) {
        super(graph, scopeFinder, actioner, options, log);
        mergeSerializer = new Serializer.Builder(graph).dependentOnly().build();
        linkResolver = new LinkResolver(graph, actioner.as(Accessor.class));

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData The raw data map
     * @param idPath   The identifiers of parent units, not including
     *                 those of the overall permission scope
     * @return the new unit
     * @throws ValidationError when data constraints are not met
     */
    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath) throws ValidationError {

        Bundle description = getDescription(itemData);

        // extractIdentifiers does not throw ValidationError on missing ID
        Bundle unit = Bundle.of(unitEntity, ImportHelpers.extractIdentifiers(itemData));

        // Get the local identifier
        final String localId = getLocalIdentifier(unit);

        // Look up the current permission scope for this item:
        PermissionScope localScope = scopeFinder.apply(localId);

        BundleManager bundleManager = getBundleManager(localScope, idPath);

        Mutation<DocumentaryUnit> mutation =
                bundleManager.createOrUpdate(mergeDescriptions(localScope, unit, description, idPath), DocumentaryUnit.class);
        logger.debug("Imported item: {}", itemData.get("name"));
        DocumentaryUnit frame = mutation.getNode();

        // If we're created some links on an otherwise unchanged item,
        // indicate this...
        int solved = linkResolver.solveUndeterminedRelationships(frame);
        if (solved > 0 && mutation.getState() == MutationState.UNCHANGED) {
            mutation = Mutation.updated(mutation.getNode());
        }

        // Set the repository/item relationship
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(localScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                Repository repository = framedGraph.frame(localScope.asVertex(), Repository.class);
                frame.setRepository(repository);
                frame.setPermissionScope(repository);
            } else if (scopeType.equals(unitEntity)) {
                DocumentaryUnit parent = framedGraph.frame(localScope.asVertex(), DocumentaryUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else {
                logger.error("Unknown scope type for documentary unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);

        return frame;
    }

    /**
     * Extract the documentary unit description bundle from the raw map data.
     * <p>
     * Note: the itemData map is mutable and should be considered an out parameter.
     *
     * @param itemData the raw data map
     * @return a description bundle
     * @throws ValidationError when data constraints are not met
     */
    protected Bundle getDescription(Map<String, Object> itemData) throws ValidationError {
        List<Map<String, Object>> extractedDates = ImportHelpers.extractDates(itemData);

        Map<String, Object> raw = ImportHelpers.extractDescription(itemData, EntityClass.DOCUMENTARY_UNIT_DESCRIPTION);

        Bundle.Builder descBuilder = Bundle.Builder.withClass(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION).addData(raw);

        // Add dates and descriptions to the bundle since they are @Dependent relations.
        for (Map<String, Object> dpb : extractedDates) {
            descBuilder.addRelation(Ontology.ENTITY_HAS_DATE, Bundle.of(EntityClass.DATE_PERIOD, dpb));
        }

        for (Map<String, Object> rel : extractRelations(itemData)) {
            logger.trace("relation found: {}", rel.get(Ontology.NAME_KEY));
            descBuilder.addRelation(Ontology.HAS_ACCESS_POINT, Bundle.of(EntityClass.ACCESS_POINT, rel));
        }

        for (Map<String, Object> dpb : ImportHelpers.extractSubNodes(Entities.MAINTENANCE_EVENT, itemData)) {
            logger.trace("maintenance event found {}", dpb);
            // dates in maintenanceEvents are not DatePeriods, they are not something to search on
            descBuilder.addRelation(Ontology.HAS_MAINTENANCE_EVENT, Bundle.of(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        Map<String, Object> unknowns = ImportHelpers.extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            StringBuilder unknownProperties = new StringBuilder();
            for (String u : unknowns.keySet()) {
                unknownProperties.append(u);
            }
            logger.trace("Unknown Properties found: {}", unknownProperties);
            descBuilder.addRelation(Ontology.HAS_UNKNOWN_PROPERTY,
                    Bundle.of(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }

        // Set the description identifier same as the source file ID,
        // which together with the lang code should form a unique
        // identifier within the item
        descBuilder.addDataValue(Ontology.IDENTIFIER_KEY, raw.get(Ontology.SOURCEFILE_KEY));
        return descBuilder.build();
    }

    /**
     * Finds any bundle in the graph with the same ObjectIdentifier.
     * If there is no bundle with this identifier, it is created.
     * If it exists and a Description in the given language exists from the same source file,
     * the description is replaced. If the description is from another source, it is added to the
     * bundle's descriptions.
     *
     * @param localScope the current permission scope for this item
     * @param unit       the DocumentaryUnit to be saved
     * @param descBundle the documentsDescription to replace any previous ones with this language
     * @param idPath     the ID path of this bundle (will be relative to the ID path of the permission scope)
     * @return A bundle with description relationships merged.
     */
    protected Bundle mergeDescriptions(PermissionScope localScope, Bundle unit, Bundle descBundle, List<String> idPath) throws ValidationError {
        final String languageOfDesc = descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION);
        final String thisSourceFileId = descBundle.getDataValue(Ontology.SOURCEFILE_KEY);

        /*
         * for some reason, the idpath from the permissionscope does not contain the parent documentary unit.
         * TODO: so for now, it is added manually
         */
        List<String> itemIdPath = Lists.newArrayList(localScope.idPath());
        itemIdPath.addAll(idPath);


        // Ensure none of the parent items (not yet saved) have an invalid
        // missing identifier
        if (itemIdPath.contains(null)) {
            throw new ValidationError(unit, Ontology.IDENTIFIER_KEY, "Parent item has missing identifier");
        }

        Bundle unitWithIds = unit.generateIds(itemIdPath);
        logger.debug("merging: docUnit's graph id = {}", unitWithIds.getId());

        // If the bundle exists, we merge
        if (manager.exists(unitWithIds.getId())) {
            try {
                // read the current item’s bundle
                Bundle oldBundle = mergeSerializer.vertexToBundle(manager.getVertex(unitWithIds.getId()));

                // Filter out dependents that a) are descriptions, b) have the same language/code.
                // If the useSourceId option is enabled this allows us to have multiple
                // descriptions in the same language if they have different source file IDs,
                // so in that case only remove those if the source ID matches.
                // I know this is confusing: TODO: improve this.
                BiPredicate<String, Bundle> filter = (relationLabel, bundle) -> {
                    String lang = bundle.getDataValue(Ontology.LANGUAGE);
                    String oldSourceFileId = bundle.getDataValue(Ontology.SOURCEFILE_KEY);
                    return relationLabel.equals(Ontology.DESCRIPTION_FOR_ENTITY)
                            && bundle.getType().equals(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                            && (lang != null && lang.equals(languageOfDesc))
                            && (!options.useSourceId || (oldSourceFileId != null && oldSourceFileId.equals(thisSourceFileId)));
                };
                Bundle filtered = oldBundle.filterRelations(filter);

                return unitWithIds
                        .withRelations(filtered.getRelations())
                        .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
            } catch (SerializationError ex) {
                throw new RuntimeException("Unexpected error reading existing item: " + unitWithIds.getId(), ex);
            } catch (ItemNotFound ex) {
                throw new RuntimeException("Failed to find existing item with key: " + unitWithIds.getId(), ex);
            }
        } else { // else we create a new bundle.
            return unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }
    }

    @SuppressWarnings("unchecked")
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : data.keySet()) {
            if (key.equals(Entities.ACCESS_POINT)) {
                //name identifier
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    if (origRelation.containsKey("type")) {
                        //try to find the original identifier
                        relationNode.put(ImportHelpers.LINK_TARGET, origRelation.get("concept"));
                        //try to find the original name
                        relationNode.put(Ontology.NAME_KEY, origRelation.get("name"));
                        relationNode.put("cvoc", origRelation.get("cvoc"));
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, origRelation.get("type"));
                    } else {
                        relationNode.put(Ontology.NAME_KEY, origRelation.get(Entities.ACCESS_POINT));
                    }
                    if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                        logger.warn("relationNode without type: {}", relationNode.get(Ontology.NAME_KEY));
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, AccessPointType.corporateBody);
                    }
                    list.add(relationNode);
                }
            } else if (key.endsWith(ACCESS_POINT)) {

                if (data.get(key) instanceof List) {
                    //type, targetUrl, targetName, notes
                    for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                        if (origRelation.isEmpty()) {
                            break;
                        }
                        Map<String, Object> relationNode = Maps.newHashMap();
                        for (String eventkey : origRelation.keySet()) {
                            if (eventkey.endsWith(ACCESS_POINT)) {
                                relationNode.put(Ontology.ACCESS_POINT_TYPE,
                                        eventkey.substring(0, eventkey.indexOf(ACCESS_POINT)));
                                relationNode.put(Ontology.NAME_KEY, origRelation.get(eventkey));
                            } else {
                                relationNode.put(eventkey, origRelation.get(eventkey));
                            }
                        }
                        if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                            relationNode.put(Ontology.ACCESS_POINT_TYPE, AccessPointType.corporateBody);
                        }
                        //if no name is given, it was apparently an empty <controlaccess> tag?
                        if (relationNode.containsKey(Ontology.NAME_KEY)) {
                            list.add(relationNode);
                        }
                    }
                } else {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    relationNode.put(Ontology.ACCESS_POINT_TYPE,
                            key.substring(0, key.indexOf(ACCESS_POINT)));
                    relationNode.put(Ontology.NAME_KEY, data.get(key));
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    @Override
    public AbstractUnit importItem(Map<String, Object> itemData) throws ValidationError {
        return importItem(itemData, new Stack<>());
    }
}
