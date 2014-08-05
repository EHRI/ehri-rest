package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EventViews {

    private static final Logger logger = LoggerFactory.getLogger(EventViews.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final ActionManager actionManager;
    private final AclManager aclManager;
    private final List<String> users;
    private final List<String> ids;
    private final List<EntityClass> entityTypes;
    private final List<EventTypes> eventTypes;
    private final Optional<String> from;
    private final Optional<String> to;
    private final List<ShowType> showType;

    public static enum ShowType {
        watched, followed
    }

    private EventViews(
            final FramedGraph<?> graph,
            final List<String> users,
            final List<String> ids,
            final List<EntityClass> entityTypes,
            final List<EventTypes> eventTypes,
            final Optional<String> from,
            final Optional<String> to,
            final List<ShowType> showType) {
        this.graph = graph;
        this.actionManager = new ActionManager(graph);
        this.aclManager = new AclManager(graph);
        this.manager = GraphManagerFactory.getInstance(graph);
        this.users = users;
        this.ids = ids;
        this.entityTypes = entityTypes;
        this.eventTypes = eventTypes;
        this.from = from;
        this.to = to;
        this.showType = showType;

    }

    public EventViews(FramedGraph<?> graph) {
        this(graph, Lists.<String>newArrayList(),
                Lists.<String>newArrayList(),
                Lists.<EntityClass>newArrayList(),
                Lists.<EventTypes>newArrayList(),
                Optional.<String>absent(),
                Optional.<String>absent(),
                Lists.<ShowType>newArrayList());
    }

    public Iterable<SystemEvent> list(Query<SystemEvent> query, Accessor accessor) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent,SystemEvent> pipe = new GremlinPipeline<SystemEvent, SystemEvent>(
                actionManager.getLatestGlobalEvents());

        // Add additional generic filters
        return query.page(filterEvents(pipe), accessor);
    }

    /**
     * List items "interesting" for a user, optionally filtered by the `ShowType`
     * to items they watch or users they follow.
     */
    public Iterable<SystemEvent> listAsUser(Query<SystemEvent> query, UserProfile asUser, Accessor accessor) {
        final PipeFunction<Vertex, Boolean> aclFilterTest = aclManager.getAclFilterFunction(asUser);

        // Set IDs to items this asUser is watching...
        final List<String> watching = Lists.newArrayList();
        for (Watchable item : asUser.getWatching()) {
            watching.add(item.getId());
        }

        final List<String> following = Lists.newArrayList();
        for (UserProfile other : asUser.getFollowing()) {
            following.add(other.getId());
        }

        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent,SystemEvent> pipe = new GremlinPipeline<SystemEvent, SystemEvent>(
                actionManager.getLatestGlobalEvents());

        // Add additional generic filters
        pipe = filterEvents(pipe);

        // Filter out those we're not watching, or are actioned
        // by users we're not following...
        if (!showType.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    if (showType.contains(ShowType.watched)) {
                        for (AccessibleEntity e : event.getSubjects()) {
                            if (watching.contains(e.getId())) {
                                return true;
                            }
                        }
                    }
                    if (showType.contains(ShowType.followed)) {
                        Actioner actioner = event.getActioner();
                        if (actioner != null && following.contains(actioner.getId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        // Filter items accessible to this asUser... hide the
        // event if any subjects or the scope are inaccessible
        // to the asUser.
        pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
            @Override
            public Boolean compute(SystemEvent event) {
                Frame eventScope = event.getEventScope();
                if (eventScope != null && !aclFilterTest.compute(eventScope.asVertex())) {
                    return false;
                }
                for (AccessibleEntity e : event.getSubjects()) {
                    if (!aclFilterTest.compute(e.asVertex())) {
                        return false;
                    }
                }
                return true;
            }
        });

        return query.page(pipe, accessor);
    }

    private GremlinPipeline<SystemEvent, SystemEvent> filterEvents(
            GremlinPipeline<SystemEvent, SystemEvent> pipe) {

        if (!eventTypes.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    return eventTypes.contains(event.getEventType());
                }
            });
        }

        if (!ids.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    for (AccessibleEntity e : event.getSubjects()) {
                        if (ids.contains(e.getId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        if (!entityTypes.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    for (AccessibleEntity e : event.getSubjects()) {
                        if (entityTypes.contains(manager.getEntityClass(e))) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        if (!users.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    Actioner actioner = event.getActioner();
                    return actioner != null && users.contains(actioner.getId());
                }
            });
        }

        // Add from/to filters (depends on timestamp strings comparing the right way!
        if (from.isPresent()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    String timestamp = event.getTimestamp();
                    return from.get().compareTo(timestamp) <= 0;
                }
            });
        }

        if (to.isPresent()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    String timestamp = event.getTimestamp();
                    return to.get().compareTo(timestamp) >= 0;
                }
            });
        }
        return pipe;
    }

    public EventViews from(String from) {
        return new EventViews(graph, users,
                ids,
                entityTypes,
                eventTypes,
                Optional.fromNullable(from),
                to,
                showType);
    }

    public EventViews to(String to) {
        return new EventViews(graph,
                users,
                ids,
                entityTypes,
                eventTypes,
                from,
                Optional.fromNullable(to),
                showType);
    }

    public EventViews withIds(String... ids) {
        return new EventViews(graph,
                users,
                Lists.newArrayList(ids),
                entityTypes,
                eventTypes,
                from,
                to,
                showType);
    }

    public EventViews withUsers(String... users) {
        return new EventViews(graph,
                Lists.newArrayList(users),
                ids,
                entityTypes,
                eventTypes,
                from,
                to,
                showType);
    }

    public EventViews withEntityClasses(EntityClass... entityTypes) {
        return new EventViews(graph,
                users,
                ids,
                Lists.newArrayList(entityTypes),
                eventTypes,
                from,
                to,
                showType);
    }

    public EventViews withEntityTypes(String... entityTypes) {
        List<EntityClass> entityClasses = Lists.newArrayList();
        for (String et : entityTypes) {
            try {
                entityClasses.add(EntityClass.withName(et));
            } catch (Exception e) {
                logger.warn("Ignoring invalid entity type: {}", et);
            }
        }
        return new EventViews(graph,
                users,
                ids,
                entityClasses,
                eventTypes,
                from,
                to,
                showType);
    }

    public EventViews withEventTypes(EventTypes... eventTypes) {
        return new EventViews(graph,
                users,
                ids,
                entityTypes,
                Lists.newArrayList(eventTypes),
                from,
                to,
                showType);
    }

    public EventViews withShowType(ShowType... type) {
        return new EventViews(graph,
                users,
                ids,
                entityTypes,
                eventTypes,
                from,
                to,
                Lists.newArrayList(type));
    }
}
