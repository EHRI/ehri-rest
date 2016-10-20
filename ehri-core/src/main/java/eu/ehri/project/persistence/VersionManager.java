package eu.ehri.project.persistence;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class VersionManager {

    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;

    public VersionManager(FramedGraph<?> graph) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * If an item was deleted, obtain the last version before it
     * was removed.
     *
     * @param id the item's ID
     * @return a version frame
     */
    public Optional<Version> versionAtDeletion(String id) {
        try (CloseableIterable<Version> versions = manager
                .getEntities(Ontology.VERSION_ENTITY_ID, id, EntityClass.VERSION, Version.class)) {
            for (Version v : versions) {
                SystemEvent event = v.getTriggeringEvent();
                if (event != null && EventTypes.deletion.equals(event.getEventType())) {
                    return Optional.of(v);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get the last version for all deleted items.
     *
     * @param type the entity class
     * @return an iterable of Version frames
     */
    public Iterable<Version> versionsAtDeletion(EntityClass type, String from, String until) {
        return Iterables.filter(manager.getEntities(Ontology.VERSION_ENTITY_CLASS, type.getName(),
                EntityClass.VERSION, Version.class), v -> {
            SystemEvent latestEvent = v.getTriggeringEvent();
            try {
                return latestEvent.getEventType().equals(EventTypes.deletion)
                        && (from == null || from.compareTo(latestEvent.getTimestamp()) < 0)
                        && (until == null || until.compareTo(latestEvent.getTimestamp()) >= 0);
            } catch (NullPointerException e) {
                // Shouldn't happen, but just to be safe...
                return false;
            }
        });
    }
}
