package eu.ehri.project.tools;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.persistence.ActionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.internal.crypto.Des;

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
    private final boolean dryrun;
    private final int maxItems;
    private final GraphManager manager;
    private final ActionManager actionManager;

    public FindReplace(FramedGraph<?> graph, boolean dryrun, int maxItems) {
        this.graph = graph;
        this.dryrun = dryrun;
        this.maxItems = maxItems;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.actionManager = new ActionManager(graph);
    }

    public FindReplace(FramedGraph<?> graph, int maxItems) {
        this(graph, true, maxItems);
    }

    public FindReplace withActualRename(boolean commit) {
        return new FindReplace(graph, !commit, maxItems);
    }

    public List<Described> findAndReplaceInDescription(
            EntityClass entityClass, String property,
            String find, String replace, Actioner actioner, String logMessage) {

        logger.info("Find: '{}'", find);
        logger.info("Replace: '{}'", replace);

        List<Described> todo = Lists.newArrayList();
        ActionManager.EventContext context = actionManager
                .newEventContext(actioner, EventTypes.modification, Optional.of(logMessage));

        try (CloseableIterable<Described> entities = manager.getEntities(entityClass, Described.class)) {
            for (Described entity : entities) {
                if (todo.size() >= maxItems) {
                    break;
                }

                for (Description description : entity.getDescriptions()) {
                    Object value = description.getProperty(property);
                    if (find(find, value)) {
                        todo.add(entity);
                        logger.info("Found in {}: {}", entity.getId(), description.getLanguageOfDescription());

                        if (!dryrun) {
                            context.createVersion(entity);
                            context.addSubjects(entity);
                            description.asVertex().setProperty(property, replace(find, replace, value));
                        }
                    }
                }
            }
        }

        if (!dryrun && !context.getSubjects().isEmpty()) {
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
