/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.importers.util;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.NodeProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.MaintenanceEventType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.utils.LanguageHelpers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Import utility class.
 */
public class ImportHelpers {

    public static final String LINK_TARGET = "target";
    public static final String OBJECT_IDENTIFIER = "objectIdentifier";

    // Keys in the node that denote unknown properties must start with the value of UNKNOWN.
    public static final String UNKNOWN_PREFIX = "UNKNOWN_";
    private static final String NODE_PROPERTIES = "allowedNodeProperties.csv";

    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    public static final String LANGUAGE_KEY_PREFIX = "language";

    private static final Logger logger = LoggerFactory.getLogger(ImportHelpers.class);
    private static final Joiner stringJoiner = Joiner.on("\n\n").skipNulls();
    private static final NodeProperties nodeProperties = loadNodeProperties();

    /**
     * Extract properties from the itemData Map that are marked as unknown, and return them in a new Map.
     *
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError never
     */
    public static Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = Maps.newHashMap();
        for (Map.Entry<String, Object> key : itemData.entrySet()) {
            if (key.getKey().startsWith(UNKNOWN_PREFIX)) {
                unknowns.put(key.getKey().substring(UNKNOWN_PREFIX.length()), key.getValue());
            }
        }
        return unknowns;
    }

    /**
     * only properties that have the multivalued-status can actually be multivalued. all other properties will be
     * flattened by this method.
     *
     * @param key    a property key
     * @param value  a property value
     * @param entity the EntityClass with which this frameMap must comply
     */
    public static Object flattenNonMultivaluedProperties(String key, Object value, EntityClass entity) {
        if (value instanceof List
                && !(nodeProperties.hasProperty(entity.getName(), key)
                && nodeProperties.isMultivaluedProperty(entity.getName(), key))) {
            logger.trace("Flattening array property value: {}: {}", key, value);
            return stringJoiner.join((List<?>) value);
        } else {
            return value;
        }
    }

    /**
     * Extract DocumentaryUnit properties from the itemData and return them as a new Map.
     * This implementation only extracts the objectIdentifier.
     *
     * @param itemData a Map containing raw properties of a DocumentaryUnit
     * @return a new Map containing the objectIdentifier property
     */
    public static Map<String, Object> extractIdentifiers(Map<String, Object> itemData) {
        Map<String, Object> unit = Maps.newHashMap();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_IDENTIFIER));
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
            logger.trace("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    /**
     * Extract a Map containing the properties of a generic description.
     * Excludes unknown properties, object identifier(s), maintenance events,
     * relations, addresses and access point relations.
     *
     * @param itemData a Map containing raw properties of the description
     * @param entity   an EntityClass
     * @return a Map representation of a generic Description
     */
    public static Map<String, Object> extractDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = Maps.newHashMap();

        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (Map.Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemProperty.getValue());
            } else if (!itemProperty.getKey().startsWith(UNKNOWN_PREFIX)
                    && !itemProperty.getKey().equals(OBJECT_IDENTIFIER)
                    && !itemProperty.getKey().equals(Ontology.IDENTIFIER_KEY)
                    && !itemProperty.getKey().equals(Ontology.OTHER_IDENTIFIERS)
                    && !itemProperty.getKey().startsWith(Entities.MAINTENANCE_EVENT)
                    && !itemProperty.getKey().startsWith(Entities.ACCESS_POINT)
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
    public static Map<String, Object> extractAddress(Map<String, Object> itemData) {
        Map<String, Object> address = Maps.newHashMap();
        for (Map.Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().startsWith("address/")) {
                address.put(itemProperty.getKey().substring(8), itemProperty.getValue());
            }
        }
        return address;
    }

    /**
     * Extract a list of entity bundles for DatePeriods from the data,
     * attempting to parse the unitdate attribute.
     *
     * @param data the data map. This is an out parameter from which
     *             keys associated with extracted dates will be removed
     */
    public static List<Map<String, Object>> extractDates(Map<String, Object> data) {
        return DateParser.extractDates(data);
    }

    /**
     * Extract the data from a sub-node.
     *
     * @param event a Map of event properties
     * @return a data map
     */
    public static Map<String, Object> getSubNode(Map<String, Object> event) {
        Map<String, Object> me = Maps.newHashMap();
        for (Map.Entry<String, Object> eventEntry : event.entrySet()) {
            // Hack for EAG 1 and 2012 compatibility - maps maintenance event
            // types from old to new values
            if (eventEntry.getKey().equals(Ontology.MAINTENANCE_EVENT_TYPE)) {
                me.put(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType
                        .withName((String) eventEntry.getValue()).toString());
            } else {
                me.put(eventEntry.getKey(), eventEntry.getValue());
            }
        }
        if (!me.containsKey(Ontology.MAINTENANCE_EVENT_TYPE)) {
            me.put(Ontology.MAINTENANCE_EVENT_TYPE, MaintenanceEventType.updated.name());
        }
        return me;
    }

    public static void overwritePropertyInGraph(Map<String, Object> c, String property, String value) {
        String normValue = normaliseValue(property, value);
        if (normValue != null && !normValue.isEmpty()) {
            logger.trace("overwrite property: {} {}", property, normValue);
            c.put(property, normValue);
        }
    }

    /**
     * Stores this property value pair in the given graph node representation.
     * If the value is effectively empty, nothing happens.
     * If the property already exists, it is added to the value list.
     *
     * @param c        a Map representation of a graph node
     * @param property the key to store the value for
     * @param value    the value to store
     */
    public static void putPropertyInGraph(Map<String, Object> c, String property, String value) {
        String normValue = normaliseValue(property, value);
        if (normValue == null || normValue.isEmpty()) {
            return;
        }
        logger.trace("putProp: {} -> {}", property, normValue);
        if (c.containsKey(property)) {
            Object currentValue = c.get(property);
            if (currentValue instanceof List) {
                ((List<Object>) currentValue).add(normValue);
            } else {
                c.put(property, Lists.newArrayList(currentValue, normValue));
            }
        } else {
            c.put(property, normValue);
        }
    }

    private static String normaliseValue(String property, String value) {
        String trimmedValue = StringUtils.normalizeSpace(value);
        // Language codes are converted to their 3-letter alternates
        return property.startsWith(LANGUAGE_KEY_PREFIX)
                ? LanguageHelpers.iso639DashTwoCode(trimmedValue)
                : trimmedValue;
    }

    public static List<Map<String, Object>> extractSubNodes(String type, Map<String, Object> data) {
        List<Map<String, Object>> out = Lists.newArrayList();
        Object nodes = data.get(type);
        if (nodes != null && nodes instanceof List) {
            for (Map<String, Object> event : (List<Map<String, Object>>) nodes) {
                out.add(getSubNode(event));
            }
        }
        return out;
    }

    // Helpers

    private static NodeProperties loadNodeProperties() {
        try (InputStream fis = ImportHelpers.class.getClassLoader().getResourceAsStream(NODE_PROPERTIES);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charsets.UTF_8))) {
            NodeProperties nodeProperties = new NodeProperties();
            String headers = br.readLine();
            nodeProperties.setTitles(headers);

            String line;
            while ((line = br.readLine()) != null) {
                nodeProperties.addRow(line);
            }
            return nodeProperties;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Missing or empty properties file: " + NODE_PROPERTIES);
        }
    }
}
