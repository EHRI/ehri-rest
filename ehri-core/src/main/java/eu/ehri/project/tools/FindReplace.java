package eu.ehri.project.tools;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Find and replace text in item properties.
 * <p>
 * Unlike a Cypher query, this class takes care of managing
 * the audit log to record changes, and also tries to prevent
 * accidental misuse.
 */
public class FindReplace {

    private static final Logger logger = LoggerFactory.getLogger(FindReplace.class);

    private final FramedGraph<?> graph;
    private final boolean commit;
    private final int maxItems;
    private final GraphManager manager;
    private final ActionManager actionManager;
    private final Serializer depSerializer;
    private final BundleManager dao;

    public FindReplace(FramedGraph<?> graph, boolean commit, int maxItems) {
        this.graph = graph;
        this.commit = commit;
        this.maxItems = maxItems;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.actionManager = new ActionManager(graph);
        this.depSerializer = new Serializer.Builder(graph).dependentOnly().build();
        this.dao = new BundleManager(graph);
    }

    /**
     * Find and replace a string
     *
     * @param contentType the content type of the top-level item
     * @param entityType  the entity type of the property-holding node
     * @param property    the name of the property to search
     * @param textToFind  the text to find
     * @param replacement the replacement text
     * @param actioner    the current user
     * @param logMessage  a mandatory log message
     * @return a list of lists each comprising: the parent item ID, the
     * child item ID, and the current text value in which a match was found
     */
    public List<List<String>> findAndReplace(
            EntityClass contentType, EntityClass entityType,
            String property, String textToFind, String replacement,
            Actioner actioner, String logMessage) throws ValidationError {

        Preconditions.checkNotNull(entityType, "Entity type cannot be null.");
        Preconditions.checkNotNull(property, "Property name cannot be null.");
        Preconditions.checkNotNull(textToFind, "Text to find cannot be null.");
        Preconditions.checkArgument(!commit || replacement != null,
                "Replacement text cannot be null if committing a replacement value.");
        Preconditions.checkArgument(!commit || logMessage != null,
                "Log message cannot be null if committing a replacement value.");

        logger.info("Find:    '{}'", textToFind);
        logger.info("Replace: '{}'", replacement);

        List<List<String>> todo = Lists.newArrayList();

        EventTypes eventType = contentType.equals(entityType)
                ? EventTypes.modification
                : EventTypes.modifyDependent;
        ActionManager.EventContext context = actionManager
                .newEventContext(actioner, eventType, Optional.ofNullable(logMessage));

        try (CloseableIterable<Accessible> entities = manager.getEntities(contentType, Accessible.class)) {
            for (Accessible entity : entities) {
                if (todo.size() >= maxItems) {
                    break;
                }

                Bundle bundle = depSerializer.entityToBundle(entity);
                List<List<String>> matches = Lists.newArrayList();
                bundle.map(d -> {
                    if (d.getType().equals(entityType)) {
                        Object v = d.getDataValue(property);
                        if (find(textToFind, v)) {
                            matches.add(Lists.newArrayList(entity.getId(),
                                    d.getId(), v.toString()));
                        }
                    }
                    return d;
                });

                if (!matches.isEmpty()) {
                    todo.addAll(matches);
                    logger.info("Found in {}", entity.getId());

                    if (commit) {
                        context.createVersion(entity);
                        context.addSubjects(entity);

                        Bundle newBundle = bundle.map(d -> {
                            if (d.getType().equals(entityType)) {
                                Object newValue = replace(textToFind, replacement, d.getDataValue(property));
                                return d.withDataValue(property, newValue);
                            }
                            return d;
                        });
                        dao.update(newBundle, Accessible.class);
                    }
                }
            }
        } catch (SerializationError | ItemNotFound e) {
            throw new RuntimeException(e);
        }

        if (commit && !context.getSubjects().isEmpty()) {
            context.commit();
        }

        return todo;
    }

    private boolean find(String needle, Object data) {
        if (data != null) {
            if (data instanceof List) {
                for (Object v : ((List) data)) {
                    if (find(needle, v)) {
                        return true;
                    }
                }
                return false;
            } else if (data instanceof String) {
                return ((String) data).contains(needle);
            }
        }
        return false;
    }

    private Object replace(String original, String replacement, Object data) {
        if (data != null) {
            if (data instanceof List) {
                List<Object> newList = Lists.newArrayListWithExpectedSize(((List) data).size());
                for (Object v : ((List) data)) {
                    newList.add(replace(original, replacement, v));
                }
                return newList;
            } else if (data instanceof String) {
                return ((String) data).replace(original, replacement);
            }
        }
        return data;
    }
}
