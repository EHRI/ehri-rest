package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class EventViews {

    private static final Logger logger = LoggerFactory.getLogger(EventViews.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final ActionManager actionManager;
    private final AclManager aclManager;
    private final Set<String> users;
    private final Set<String> ids;
    private final Set<EntityClass> entityTypes;
    private final Set<EventTypes> eventTypes;
    private final Optional<String> from;
    private final Optional<String> to;
    private final Set<ShowType> showType;

    public static enum ShowType {
        watched, followed
    }

    private EventViews(
            final FramedGraph<?> graph,
            final Collection<String> users,
            final Collection<String> ids,
            final Collection<EntityClass> entityTypes,
            final Collection<EventTypes> eventTypes,
            final Optional<String> from,
            final Optional<String> to,
            final Collection<ShowType> showType) {
        this.graph = graph;
        this.actionManager = new ActionManager(graph);
        this.aclManager = new AclManager(graph);
        this.manager = GraphManagerFactory.getInstance(graph);
        this.users = Sets.newHashSet(users);
        this.ids = Sets.newHashSet(ids);
        this.entityTypes = Sets.newEnumSet(entityTypes, EntityClass.class);
        this.eventTypes = Sets.newEnumSet(eventTypes, EventTypes.class);
        this.from = from;
        this.to = to;
        this.showType = Sets.newEnumSet(showType, ShowType.class);

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
        return query.setStream(true).page(applyAclFilter(filterEvents(pipe), accessor), accessor);
    }

    /**
     * List items "interesting" for a user, optionally filtered by the `ShowType`
     * to items they watch or users they follow.
     */
    public Iterable<SystemEvent> listAsUser(Query<SystemEvent> query, UserProfile asUser, Accessor accessor) {

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

        return query.page(applyAclFilter(pipe, asUser), accessor);
    }

    public Iterable<SystemEvent> listByUser(Query<SystemEvent> query, UserProfile byUser, Accessor user) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent,SystemEvent> pipe = new GremlinPipeline<SystemEvent, SystemEvent>(
                manager.cast(byUser, Actioner.class).getActions());

        // Add additional generic filters
        return applyAclFilter(filterEvents(pipe), user);
    }

    private GremlinPipeline<SystemEvent, SystemEvent> applyAclFilter(GremlinPipeline<SystemEvent, SystemEvent> pipe,
            Accessor asUser) {
        final PipeFunction<Vertex, Boolean> aclFilterTest = aclManager.getAclFilterFunction(asUser);

        // Filter items accessible to this asUser... hide the
        // event if any subjects or the scope are inaccessible
        // to the asUser.
        return pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
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
    }

    private GremlinPipeline<SystemEvent, SystemEvent> filterEvents(
            GremlinPipeline<SystemEvent, SystemEvent> pipe) {

        if (!eventTypes.isEmpty()) {
            System.out.println("Applying event type filter...");
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
