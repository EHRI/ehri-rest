package eu.ehri.project.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Single-use code to upgrade the DB schema from version
 * 1 to version 2.
 * <p>
 * Ideally, we'd use some declarative script for this, and were
 * it not for the JSON data in Version nodes that represents
 * prior incarnations of items, we could.
 * <p>
 * This function de-serializes the serialized data, upgrades it
 * to the new format, and serializes it again.
 */
public class DbUpgrader1to2 {
    public static final Map<String, String> changeMap = ImmutableMap.<String, String>builder()
            .put("historicalAgent", "HistoricalAgent")
            .put("repository", "Repository")
            .put("documentaryUnit", "DocumentaryUnit")
            .put("virtualUnit", "VirtualUnit")
            .put("systemEvent", "SystemEvent")
            .put("documentDescription", "DocumentaryUnitDescription")
            .put("repositoryDescription", "RepositoryDescription")
            .put("historicalAgentDescription", "HistoricalAgentDescription")
            .put("group", "Group")
            .put("country", "Country")
            .put("userProfile", "UserProfile")
            .put("datePeriod", "DatePeriod")
            .put("annotation", "Annotation")
            .put("address", "Address")
            .put("property", "UnknownProperty")
            .put("permission", "Permission")
            .put("permissionGrant", "PermissionGrant")
            .put("contentType", "ContentType")
            .put("version", "Version")
            .put("link", "Link")
            .put("cvocVocabulary", "CvocVocabulary")
            .put("cvocConcept", "CvocConcept")
            .put("cvocConceptDescription", "CvocConceptDescription")
            .put("system", "System")
            .put("maintenanceEvent", "MaintenanceEvent")
            .put("relationship", "AccessPoint")
            .put("authoritativeSet", "AuthoritativeSet")
            .put("eventLink", "EventLink")
            .build();

    public static final String OLD_ID_KEY = "__ID__";
    public static final String OLD_TYPE_KEY = "__ISA__";

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DbUpgrader1to2.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final OnChange onChange;

    public DbUpgrader1to2(FramedGraph<?> graph, OnChange onChange) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.onChange = onChange;
    }

    public interface OnChange {
        void changed();
    }

    public DbUpgrader1to2 setDbSchemaVersion() {
        try {
            Vertex vertex = manager.getVertex(ActionManager.GLOBAL_EVENT_ROOT);
            vertex.setProperty("DB_SCHEMA", "2");
            vertex.setProperty("DB_SCHEMA_DATE", DateTime.now().toString());
            onChange.changed();
            return this;
        } catch (ItemNotFound e) {
            throw new RuntimeException("Global Event Root not found!", e);
        }
    }

    public DbUpgrader1to2 upgradeIdAndTypeKeys() {
        for (Vertex v : graph.getVertices()) {
            Object oldId = v.getProperty(OLD_ID_KEY);
            if (oldId != null) {
                v.setProperty(EntityType.ID_KEY, oldId);
                v.removeProperty(OLD_ID_KEY);
            }
            Object oldType = v.getProperty(OLD_TYPE_KEY);
            if (oldType != null) {
                v.setProperty(EntityType.TYPE_KEY, oldType);
                v.removeProperty(OLD_TYPE_KEY);
            }

            onChange.changed();
        }
        return this;
    }

    public DbUpgrader1to2 setIdAndTypeOnEventLinks() {
        for (Vertex v : graph.getVertices()) {
            String dt = v.getProperty(ActionManager.DEBUG_TYPE);
            if (ActionManager.EVENT_LINK.equalsIgnoreCase(dt)) {
                String id = manager.getId(v);
                if (id == null) {
                    UUID timeBasedUUID = GenericIdGenerator.getTimeBasedUUID();
                    v.setProperty(EntityType.ID_KEY, timeBasedUUID.toString());
                    v.setProperty(EntityType.TYPE_KEY, Entities.EVENT_LINK);
                    onChange.changed();
                }
            }
        }
        return this;
    }

    public DbUpgrader1to2 upgradeTypeValues() throws IOException {
        for (Vertex v : graph.getVertices()) {
            String oldType = v.getProperty(EntityType.TYPE_KEY);
            if (changeMap.containsKey(oldType)) {
                String newType = changeMap.get(oldType);
                v.setProperty(EntityType.TYPE_KEY, newType);

                // Content types have the type name as their ID, so rename
                // those at the same time.
                if (newType.equals(Entities.CONTENT_TYPE)) {
                    String oldTypeId = v.getProperty(EntityType.ID_KEY);
                    String newTypeId = changeMap.get(oldTypeId);
                    v.setProperty(EntityType.ID_KEY, newTypeId);
                } else if (newType.equals(Entities.VERSION)) {
                    String entityData = v.getProperty(Ontology.VERSION_ENTITY_DATA);
                    JsonNode node = jsonMapper.readTree(entityData);
                    if (!node.isObject()) {
                        throw new RuntimeException("Unexpected JSON object: " + node.getNodeType());
                    }
                    ObjectNode before = jsonMapper.valueToTree(node);
                    ObjectNode updated = upgradeNode(before);
                    String after = jsonMapper.writeValueAsString(updated);

                    manager.setProperty(v, Ontology.VERSION_ENTITY_CLASS, newType);
                    manager.setProperty(v, Ontology.VERSION_ENTITY_DATA, after);
                }

                onChange.changed();
            }
        }
        return this;
    }

    /**
     * Recursively updates types in a JSON bundle structure,
     * returning a new node.
     *
     * @param beforeNode the original JSON
     * @return the transformed JSON
     */
    public static ObjectNode upgradeNode(ObjectNode beforeNode) {
        ObjectNode node = beforeNode.deepCopy();
        JsonNode typeNode = node.path(Bundle.TYPE_KEY);
        String typeText = typeNode.asText();
        if (typeNode.isTextual() && changeMap.containsKey(typeText)) {
            logger.trace("Renaming type key: {} -> {}", typeText, changeMap.get(typeText));
            node.set(Bundle.TYPE_KEY, jsonMapper.valueToTree(changeMap.get(typeText)));
            JsonNode rels = node.path(Bundle.REL_KEY);
            if (!rels.isMissingNode()) {
                Preconditions.checkState(rels.isObject(), "Relations is not a map: " + rels.getNodeType());
                ObjectNode relObject = jsonMapper.valueToTree(rels);
                Iterator<Map.Entry<String, JsonNode>> fields = relObject.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> next = fields.next();
                    JsonNode listNode = next.getValue();
                    Preconditions.checkState(listNode.isArray(), "Relations contains a list");
                    ArrayNode relList = jsonMapper.valueToTree(listNode);
                    for (int i = 0; i < relList.size(); i++) {
                        ObjectNode out = upgradeNode(jsonMapper.valueToTree(relList.path(i)));
                        relList.set(i, out);
                    }
                    relObject.set(next.getKey(), relList);
                }
                node.set(Bundle.REL_KEY, relObject);
            }
        }
        return node;
    }
}
