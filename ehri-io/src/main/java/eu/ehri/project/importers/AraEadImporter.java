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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 * <p>
 * will preserve existing 'otherIdentifiers' on the DocumentaryUnit.
 * <p>
 * Furthermore, it will always try to resolve the UndeterminedRelationships, not just on creation.
 * This is not standard behaviour, so use with caution.
 * <p>
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 */
public class AraEadImporter extends EadImporter {

    private static final Logger logger = LoggerFactory.getLogger(AraEadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private final EntityClass unitEntity = EntityClass.DOCUMENTARY_UNIT;
    private final Serializer mergeSerializer;

    /**
     * Construct an EadImporter object.
     *
     * @param graph           the framed graph
     * @param permissionScope the permission scope
     * @param log             the import log
     */
    public AraEadImporter(FramedGraph<?> graph, PermissionScope permissionScope, ImportLog log) {
        super(graph, permissionScope, log);
        mergeSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData The data map
     * @param idPath   The identifiers of parent documents, not including those of the overall permission scope
     * @throws ValidationError when the itemData does not contain an identifier for the unit or...
     */
    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        List<Map<String, Object>> extractedDates = extractDates(itemData);
        replaceDates(itemData, extractedDates);

        Bundle descBundle = new Bundle(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractedDates) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        for (Map<String, Object> rel : extractRelations(itemData)) {//, (String) unit.getErrors().get(IdentifiableEntity.IDENTIFIER_KEY)
            logger.debug("relation found: " + rel.get(Ontology.NAME_KEY));
            for (String s : rel.keySet()) {
                logger.debug(s);
            }
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.ACCESS_POINT, rel));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            StringBuilder unknownProperties = new StringBuilder();
            for (String u : unknowns.keySet()) {
                unknownProperties.append(u);
            }
            logger.info("Unknown Properties found: " + unknownProperties);
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        // extractDocumentaryUnit does not throw ValidationError on missing ID
        Bundle unit = new Bundle(unitEntity, extractDocumentaryUnit(itemData));


        // Check for missing identifier, throw an exception when there is no ID.
        if (unit.getDataValue(Ontology.IDENTIFIER_KEY) == null) {
            throw new ValidationError(unit, Ontology.IDENTIFIER_KEY,
                    "Missing identifier " + Ontology.IDENTIFIER_KEY);
        }
        logger.debug("Imported item: " + itemData.get("name"));

        Mutation<DocumentaryUnit> mutation =
                persister.createOrUpdate(mergeWithPreviousAndSave(unit, descBundle, idPath), DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        // Set the repository/item relationship
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(permissionScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                Repository repository = framedGraph.frame(permissionScope.asVertex(), Repository.class);
                frame.setRepository(repository);
                frame.setPermissionScope(repository);
            } else if (scopeType.equals(unitEntity)) {
                DocumentaryUnit parent = framedGraph.frame(permissionScope.asVertex(), DocumentaryUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else {
                logger.error("Unknown scope type for documentary unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);
        logger.debug("============== " + frame.getIdentifier() + " created:" + mutation.created());

        //BEWARE: it will always try to solve the UndeterminedRelationships, not only on creation!
//        if (mutation.created()) {
        solveUndeterminedRelationships(frame, descBundle);
//        }
        return frame;


    }

    /**
     * finds any bundle in the graph with the same ObjectIdentifier. if it exists it replaces the Description in the
     * given language, else it just saves it
     *
     * @param unit       - the DocumentaryUnit to be saved
     * @param descBundle - the documentsDescription to replace any previous ones with this language
     * @return A bundle with description relationships merged.
     * @throws ValidationError
     */
    protected Bundle mergeWithPreviousAndSave(Bundle unit, Bundle descBundle, List<String> idPath) throws ValidationError {
        final String languageOfDesc = descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION);
        final String thisSourceFileId = descBundle.getDataValue(Ontology.SOURCEFILE_KEY);
        /*
         * for some reason, the idpath from the permissionscope does not contain the parent documentary unit.
         * TODO: so for now, it is added manually
         */
        List<String> lpath = Lists.newArrayList();
        for (String p : getPermissionScope().idPath()) {
            lpath.add(p);
        }
        for (String p : idPath) {
            lpath.add(p);
        }
        Bundle withIds = unit.generateIds(lpath);

        if (manager.exists(withIds.getId())) {
            try {
                //read the current item’s bundle
                Bundle oldBundle = mergeSerializer
                        .vertexFrameToBundle(manager.getVertex(withIds.getId()));

                //determine if previous existing DocUnit had 'otherIdentifiers', if so, add to existing withIds
                if (oldBundle.getData().keySet().contains(Ontology.OTHER_IDENTIFIERS)) {
                    Object otherIdentifiers = oldBundle.getData().get(Ontology.OTHER_IDENTIFIERS);
                    if (unit.getData().keySet().contains(Ontology.OTHER_IDENTIFIERS)) {
                        if (otherIdentifiers instanceof List) {
                            ((List<String>) otherIdentifiers).add(unit.getDataValue(Ontology.OTHER_IDENTIFIERS).toString());
                        } else if (otherIdentifiers instanceof String) {
                            List<String> allOtherIdentifiers = Lists.newArrayList();
                            allOtherIdentifiers.add(otherIdentifiers.toString());
                            allOtherIdentifiers.add(unit.getDataValue(Ontology.OTHER_IDENTIFIERS).toString());
                            otherIdentifiers = allOtherIdentifiers;
                        }
                    }
                    withIds = withIds.withDataValue(Ontology.OTHER_IDENTIFIERS, otherIdentifiers);
                }


                //if the unit exists, with a desc with the same sourcefileid, overwrite, else create new desc
                //filter out dependents that a) are descriptions, b) have the same language/code
                Bundle.Filter filter = new Bundle.Filter() {
                    @Override
                    public boolean remove(String relationLabel, Bundle bundle) {
                        String lang = bundle.getDataValue(Ontology.LANGUAGE);
                        String oldSourceFileId = bundle.getDataValue(Ontology.SOURCEFILE_KEY);
                        return bundle.getType().equals(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                                && (lang != null
                                && lang.equals(languageOfDesc)
                                && (oldSourceFileId != null && oldSourceFileId.equals(thisSourceFileId)));
                    }
                };
                Bundle filtered = oldBundle.filterRelations(filter);

                //if this desc-id already exists, but with a different sourceFileId, 
                //change the desc-id
                String defaultDescIdentifier = withIds.getId() + "-" + languageOfDesc.toLowerCase();
                String newDescIdentifier = withIds.getId() + "-" + thisSourceFileId.toLowerCase().replace("#", "-");
                if (manager.exists(newDescIdentifier)) {
                    descBundle = descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                } else if (manager.exists(defaultDescIdentifier)) {
                    Bundle oldDescBundle = mergeSerializer
                            .vertexFrameToBundle(manager.getVertex(defaultDescIdentifier));
                    //if the previous had NO sourcefile_key OR it was different:
                    if (oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY) == null
                            || !thisSourceFileId.equals(oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY).toString())) {
                        descBundle = descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                        logger.info("other description found (" + defaultDescIdentifier + "), creating new description id: " + descBundle.getDataValue(Ontology.IDENTIFIER_KEY));
                    }
                }

                return withIds.withRelations(filtered.getRelations())
                        .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

            } catch (SerializationError ex) {
                throw new ValidationError(unit, "serialization error", ex.getMessage());
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, "item not found exception", ex.getMessage());
            }
        } else {
            return unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }
    }
}
