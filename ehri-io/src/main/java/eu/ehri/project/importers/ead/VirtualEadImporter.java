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
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.importers.util.ImportHelpers.OBJECT_IDENTIFIER;

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
@Deprecated
public class VirtualEadImporter extends EadImporter {

    private static final String REPOID = "vcRepository";
    private static final Logger logger = LoggerFactory.getLogger(VirtualEadImporter.class);

    public VirtualEadImporter(FramedGraph<?> graph, PermissionScopeFinder scopeFinder,
                              Actioner actioner, ImportOptions options, ImportLog log) {
        super(graph, scopeFinder, actioner, options, log);
    }

    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        Bundle unit = Bundle.of(EntityClass.VIRTUAL_UNIT, extractVirtualUnit(itemData));

        if (isVirtualLevel(itemData)) {
            final String localId = getLocalIdentifier(unit);
            PermissionScope localScope = scopeFinder.apply(localId);

            BundleManager bundleManager = getBundleManager(localScope, idPath);
            Bundle description = getDescription(itemData);

            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, description);
            Mutation<VirtualUnit> mutation = bundleManager.createOrUpdate(unit, VirtualUnit.class);
            VirtualUnit frame = mutation.getNode();
            logger.debug("Imported item: {}", itemData.get(Ontology.NAME_KEY));
            // Set the repository/item relationship
            //TODO: figure out another way to determine we're at the root, so we can get rid of the depth param
            if (idPath.isEmpty() && mutation.created()) {
                EntityClass scopeType = manager.getEntityClass(localScope);
                if (scopeType.equals(EntityClass.USER_PROFILE)) {
                    UserProfile responsibleUser = localScope.as(UserProfile.class);
                    frame.setAuthor(responsibleUser);
                    //the top Virtual Unit does not have a localScope.
                } else if (scopeType.equals(EntityClass.VIRTUAL_UNIT)) {
                    VirtualUnit parent = framedGraph.frame(localScope.asVertex(), VirtualUnit.class);
                    parent.addChild(frame);
                    frame.setPermissionScope(parent);
                } else {
                    logger.error("Unknown scope type for virtual unit: {}", scopeType);
                }
            }
            handleCallbacks(mutation);
            return frame;
        } else {
            try {
                //find the DocumentaryUnit using the repository_id/unit_id combo
                return findReferredToDocumentaryUnit(itemData);
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, ex.getId(), ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        String rel = "AccessPoint";
        List<Map<String, Object>> list = Lists.newArrayList();
        for (String key : data.keySet()) {
            if (key.endsWith(rel)) {
                logger.debug("{} found in data", key);
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (String eventkey : origRelation.keySet()) {
                        logger.debug(eventkey);
                        if (eventkey.endsWith(rel)) {
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

    private Map<String, Object> extractVirtualUnit(Map<String, Object> itemData) {
        Map<String, Object> unit = Maps.newHashMap();
        if (itemData.get(OBJECT_IDENTIFIER) != null) {
            unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_IDENTIFIER));
        }
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
            logger.warn("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    @Override
    public AbstractUnit importItem(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private boolean isVirtualLevel(Map<String, Object> itemData) {
        return !(itemData.containsKey(REPOID) && itemData.containsKey(OBJECT_IDENTIFIER));
    }

    private DocumentaryUnit findReferredToDocumentaryUnit(Map<String, Object> itemData) throws ItemNotFound {
        if (itemData.containsKey(REPOID) && itemData.containsKey(OBJECT_IDENTIFIER)) {
            String repositoryId = itemData.get(REPOID).toString();
            String unitId = itemData.get(OBJECT_IDENTIFIER).toString();
            Repository repository = manager.getEntity(repositoryId, Repository.class);
            for (DocumentaryUnit unit : repository.getAllDocumentaryUnits()) {
                logger.trace("{} / {} / {}", unit.getIdentifier(), unit.getId(), unitId);
                if (unit.getIdentifier().equals(unitId)) {
                    return unit;
                }
            }
            throw new ItemNotFound(String.format("No item %s found in repo %s", unitId, repositoryId));
        }
        throw new ItemNotFound("Apparently no repositoryid/unitid combo given");
    }
}
