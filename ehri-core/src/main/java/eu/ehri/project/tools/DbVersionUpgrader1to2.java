package eu.ehri.project.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single-use code to upgrade the DB schema from version
 * 1 to version 2.
 * <p/>
 * Ideally, we'd use some declarative script for this, and were
 * it not for the JSON data in Version nodes that represents
 * prior incarnations of items, we could.
 * <p/>
 * This function de-serializes the serialized data, upgrades it
 * to the new format, and serializes it again.
 */
public class DbVersionUpgrader1to2 {
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

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(DbVersionUpgrader1to2.class);

    private final FramedGraph<?> graph;

    public DbVersionUpgrader1to2(FramedGraph<?> graph) {
        this.graph = graph;

    }

    public void runUpgrade(Function<Integer,Integer> commit) throws IOException {
        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Set<String> values = Sets.newHashSet(changeMap.values());

        Integer num = 0;
        for (Version version : manager.getFrames(EntityClass.VERSION, Version.class)) {
            String entityType = version.getEntityType();
            if (changeMap.containsKey(entityType)) {
                String entityData = version.getEntityData();
                JsonNode node = jsonMapper.readTree(entityData);
                if (!node.isObject()) {
                    throw new RuntimeException("Unexpected JSON object: " + node.getNodeType());
                }
                ObjectNode before = jsonMapper.valueToTree(node);
                ObjectNode updated = upgradeNode(before);
                String after = jsonMapper.writeValueAsString(updated);

                Vertex v = version.asVertex();
                manager.setProperty(v, Ontology.VERSION_ENTITY_CLASS, changeMap.get(entityType));
                manager.setProperty(v, Ontology.VERSION_ENTITY_DATA, after);

                commit.apply(num);
            } else {
                if (!values.contains(entityType)) {
                    throw new RuntimeException("Unknown version entity type "
                            + entityType);
                }
            }
        }
    }

    /**
     * Recursively updates types in a JSON bundle structure,
     * returning a new node.
     *
     * @param beforeNode the original JSON
     * @return the transformed JSON
     */
    public ObjectNode upgradeNode(ObjectNode beforeNode) {
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
                        ObjectNode out = upgradeNode(jsonMapper.<ObjectNode>valueToTree(relList.path(i)));
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
