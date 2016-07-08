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

package eu.ehri.project.importers.base;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Import Map based representations of EAD or EAC for a given repository into the database.
 */
public abstract class SaxXmlImporter extends MapImporter {

    private static final Logger logger = LoggerFactory.getLogger(SaxXmlImporter.class);
    protected static final String LINK_TARGET = "target";

    public static final String RESOLVED_LINK_DESC = "Link provided by data provider.";

    /**
     * Construct an EadImporter object.
     *
     * @param graph           the framed graph
     * @param permissionScope the permission scope
     * @param log             the import log
     */
    public SaxXmlImporter(FramedGraph<?> graph, PermissionScope permissionScope, Actioner actioner, ImportLog log) {
        super(graph, permissionScope, actioner, log);
    }

    /**
     * Extract properties from the itemData Map that belong to a generic unit and
     * returns them as a new Map. Calls extractDocumentaryUnit.
     *
     * @param itemData a Map representation of a unit
     * @return a new Map containing those properties that are specific to a unit
     * @throws ValidationError when extractDocumentaryUnit throws it
     */
    protected Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = extractDocumentaryUnit(itemData);
        unit.put("typeOfEntity", itemData.get("typeOfEntity"));
        return unit;
    }

    /**
     * Extract DocumentaryUnit properties from the itemData and return them as a new Map.
     * This implementation only extracts the objectIdentifier.
     * <p/>
     * This implementation does not throw ValidationErrors.
     *
     * @param itemData a Map containing raw properties of a DocumentaryUnit
     * @return a new Map containing the objectIdentifier property
     * @throws ValidationError never
     */
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = Maps.newHashMap();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_IDENTIFIER));
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
            logger.debug("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    /**
     * Extract properties from the itemData Map that are marked as unknown, and return them in a new Map.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError never
     */
    protected Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = Maps.newHashMap();
        for (Entry<String, Object> key : itemData.entrySet()) {
            if (key.getKey().startsWith(SaxXmlHandler.UNKNOWN)) {
                unknowns.put(key.getKey().substring(SaxXmlHandler.UNKNOWN.length()), key.getValue());
            }
        }
        return unknowns;
    }

    /**
     * Extract node representations for related nodes based on the 'relation' property in the supplied data Map.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return an Iterable of new Maps representing related nodes and their types
     */
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        String relName = "relation";
        List<Map<String, Object>> listOfRelations = Lists.newArrayList();
        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals(relName)) {
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemProperty.getValue()) {
                    Map<String, Object> relationNode = Maps.newHashMap();
                    for (Entry<String, Object> relationProperty : origRelation.entrySet()) {
                        if (relationProperty.getKey().equals(relName + "/type")) {
                            relationNode.put(Ontology.ACCESS_POINT_TYPE, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(relName + "/url")) {
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(relName + "/" + Ontology.NAME_KEY)) {
                            //try to find the original identifier
                            relationNode.put(Ontology.NAME_KEY, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(relName + "/notes")) {
                            relationNode.put(Ontology.LINK_HAS_DESCRIPTION, relationProperty.getValue());
                        } else {
                            relationNode.put(relationProperty.getKey(), relationProperty.getValue());
                        }
                    }
                    // Set a default relationship type if no type was found in the relationship
                    if (!relationNode.containsKey(Ontology.ACCESS_POINT_TYPE)) {
                        relationNode.put(Ontology.ACCESS_POINT_TYPE, AccessPointType.corporateBody);
                    }
                    listOfRelations.add(relationNode);
                }
            }
        }
        return listOfRelations;
    }

    /**
     * Extract a Map containing the properties of a documentary unit's description.
     * Excludes unknown properties, object identifier(s), maintenance events, relations,
     * addresses and *Access relations.
     *
     * @param itemData a Map containing raw properties of a unit
     * @param entity   an EntityClass to get the multi-valuedness of properties for
     * @return a Map representation of a DocumentDescription
     */
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = Maps.newHashMap();

        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemProperty.getValue());
            } else if (itemProperty.getKey().equals("conditionsOfAccess")) {
                description.put(itemProperty.getKey(), flattenNonMultivaluedProperties(
                        itemProperty.getKey(), itemProperty.getValue(), entity));
            } else if (!itemProperty.getKey().startsWith(SaxXmlHandler.UNKNOWN)
                    && !itemProperty.getKey().equals(OBJECT_IDENTIFIER)
                    && !itemProperty.getKey().equals(Ontology.IDENTIFIER_KEY)
                    && !itemProperty.getKey().equals(Ontology.OTHER_IDENTIFIERS)
                    && !itemProperty.getKey().startsWith("maintenanceEvent")
                    && !itemProperty.getKey().startsWith("relation")
                    && !itemProperty.getKey().startsWith("IGNORE")
                    && !itemProperty.getKey().startsWith("address/")
                    && !itemProperty.getKey().endsWith("AccessPoint")) {
                description.put(itemProperty.getKey(), flattenNonMultivaluedProperties(
                        itemProperty.getKey(), itemProperty.getValue(), entity));
            }
        }

        return description;
    }

    /**
     * Extract an address node representation from the itemData.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all address/ keys (may be empty)
     */
    protected Map<String, Object> extractAddress(Map<String, Object> itemData) {
        Map<String, Object> address = Maps.newHashMap();
        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().startsWith("address/")) {
                address.put(itemProperty.getKey().substring(8), itemProperty.getValue());
            }
        }
        return address;
    }
}
