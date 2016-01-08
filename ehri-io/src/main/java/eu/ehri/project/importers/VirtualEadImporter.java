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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Import EAD describing a Virtual Collection. some rules governing virtual collections:
 * <p>
 * the archdesc should describe the purpose of this vc. it can not in itself refer to a DU.
 * <p>
 * every c level is either 1) a virtual level (=VirtualLevel), or 2) it points to an existing DocumentaryUnit
 * (=VirtualReferrer) (and consequently to the entire subtree beneath it) 1) there is no repository-tag with a
 * ehri-label
 * <p>
 * 2) there is exactly one repository-tag with an ehri-label &lt;repository
 * label="ehri_repository_vc"&gt;il-002777&lt;/repository&gt; (this will not be shown in the portal) and exactly one unitid with
 * a ehri-main-identifier label, that is identical to the existing unitid within the graph for this repository
 * <p>
 * all other tags will be ignored, since the DocumentsDescription of the referred DocumentaryUnit will be shown. there
 * should not be any c-levels beneath such a c-level
 */
public class VirtualEadImporter extends EaImporter {

    protected static final String REPOID = "vcRepository";
    protected static final String UNITID = "objectIdentifier";
    private static final Logger logger = LoggerFactory.getLogger(VirtualEadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private EntityClass unitEntity = EntityClass.VIRTUAL_UNIT;

    /**
     * Construct an EadImporter object.
     *
     * @param graph           the framed graph
     * @param permissionScope the permission scope
     * @param log             the import log
     */
    public VirtualEadImporter(FramedGraph<?> graph, PermissionScope permissionScope, ImportLog log) {
        super(graph, permissionScope, log);

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth. this will import the
     * structure as VirtualUnits, which either have a DocDescription (VirtualLevel, like series) or they point to an
     * existing DocDesc from an existing DocumentaryUnit (VirtualReferrer).
     *
     * @param itemData The data map
     * @param idPath   The identifiers of parent documents, not including those of the overall permission scope
     * @throws ValidationError when the itemData does not contain an identifier for the unit or...
     */
    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        boolean isVirtualLevel = isVirtualLevel(itemData);
        Bundle unit = new Bundle(unitEntity, extractVirtualUnit(itemData));

        if (isVirtualLevel) { //VirtualLevel
            // Check for missing identifier, throw an exception when there is no ID.
            if (unit.getDataValue(Ontology.IDENTIFIER_KEY) == null) {
                throw new ValidationError(unit, Ontology.IDENTIFIER_KEY,
                        "Missing identifier " + Ontology.IDENTIFIER_KEY);
            }
            logger.debug("Imported item: " + itemData.get("name"));
            Bundle descBundle = new Bundle(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
            // Add dates and descriptions to the bundle since they're @Dependent
            // relations.
            for (Map<String, Object> dpb : extractDates(itemData)) {
                descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
            }
            for (Map<String, Object> rel : extractRelations(itemData)) {//, (String) unit.getErrors().get(Identifiable.IDENTIFIER_KEY)
                logger.debug("relation found: " + rel.get(Ontology.NAME_KEY));
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.ACCESS_POINT, rel));
            }
            Map<String, Object> unknowns = extractUnknownProperties(itemData);
            if (!unknowns.isEmpty()) {
                logger.debug("Unknown Properties found");
                descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
            }
            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
            Mutation<VirtualUnit> mutation =
                    persister.createOrUpdate(unit, VirtualUnit.class);
            VirtualUnit frame = mutation.getNode();
            // Set the repository/item relationship
            //TODO: figure out another way to determine we're at the root, so we can get rid of the depth param
            if (idPath.isEmpty() && mutation.created()) {
                EntityClass scopeType = manager.getEntityClass(permissionScope);
                if (scopeType.equals(EntityClass.USER_PROFILE)) {
                    UserProfile responsibleUser = framedGraph.frame(permissionScope.asVertex(), UserProfile.class);
                    frame.setAuthor(responsibleUser);
                    //the top Virtual Unit does not have a permissionScope. 
                } else if (scopeType.equals(unitEntity)) {
                    VirtualUnit parent = framedGraph.frame(permissionScope.asVertex(), VirtualUnit.class);
                    parent.addChild(frame);
                    frame.setPermissionScope(parent);
                } else {
                    logger.error("Unknown scope type for virtual unit: {}", scopeType);
                }
            }
            handleCallbacks(mutation);
            return frame;
        } else { //find the referred Documentary Unit and RETURN it
            try {
                //find the DocumentaryUnit using the repository_id/unit_id combo
                return findReferredToDocumentaryUnit(itemData);
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, ex.getKey(), ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        String REL = "AccessPoint";
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : data.keySet()) {
            if (key.endsWith(REL)) {
                logger.debug(key + " found in data");
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (String eventkey : origRelation.keySet()) {
                        logger.debug(eventkey);
                        if (eventkey.endsWith(REL)) {
                            relationNode.put(Ontology.ACCESS_POINT_TYPE, eventkey);
                            relationNode.put(Ontology.NAME_KEY, origRelation.get(eventkey));
                        } else {
                            relationNode.put(eventkey, origRelation.get(eventkey));
                        }
                    }
                    if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, "corporateBodyAccessPoint");
                    }
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    /**
     * Creates a Map containing properties of a Virtual Unit.
     * <p>
     * These properties are the unit's identifiers.
     *
     * @param itemData Map of all extracted information
     * @return a Map representing a Documentary Unit node
     * @throws ValidationError
     */
    protected Map<String, Object> extractVirtualUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = Maps.newHashMap();
        if (itemData.get(OBJECT_ID) != null) {
            unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_ID));
        }
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
            logger.debug("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    @Override
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    /**
     * Creates a Map containing properties of a Documentary Unit description. These properties are the unit
     * description's properties: all except the doc unit identifiers and unknown properties.
     *
     * @param itemData Map of all extracted information
     * @param depth    depth of node in the tree
     * @return a Map representing a Documentary Unit Description node
     * @throws ValidationError
     */
    protected Map<String, Object> extractDocumentDescription(Map<String, Object> itemData, int depth) throws ValidationError {

        Map<String, Object> unit = Maps.newHashMap();
        for (String key : itemData.keySet()) {
            if (!(key.equals(OBJECT_ID)
                    || key.equals(Ontology.OTHER_IDENTIFIERS)
                    || key.startsWith(SaxXmlHandler.UNKNOWN))) {
                unit.put(key, itemData.get(key));
            }
        }
        return unit;
    }

    @Override
    public Accessible importItem(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void importAsVirtualCollection() {
        unitEntity = EntityClass.VIRTUAL_UNIT;
    }

    private boolean isVirtualLevel(Map<String, Object> itemData) {
        return !(itemData.containsKey(REPOID) && itemData.containsKey(UNITID));
    }

    private DocumentaryUnit findReferredToDocumentaryUnit(Map<String, Object> itemData) throws ItemNotFound {
        if (itemData.containsKey(REPOID) && itemData.containsKey(UNITID)) {
            String repositoryid = itemData.get(REPOID).toString();
            String unitid = itemData.get(UNITID).toString();
            Repository repository = manager.getEntity(repositoryid, Repository.class);
            for (DocumentaryUnit unit : repository.getAllCollections()) {
                logger.debug(unit.getIdentifier() + " " + unit.getId() + " " + unitid);
                if (unit.getIdentifier().equals(unitid)) {
                    return unit;
                }
            }
            throw new ItemNotFound(String.format("No item %s found in repo %s", unitid, repositoryid));
        }
        throw new ItemNotFound("Apparently no repositoryid/unitid combo given");

    }
}
