package eu.ehri.extension;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.ActionManager;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTful interface for the Event class. Note: Event instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path(Entities.SYSTEM_EVENT)
public class EventResource extends AbstractAccessibleEntityResource<SystemEvent> {

    public final static String ITEM_TYPE_PARAM = "type";
    public final static String ITEM_ID_PARAM = "item";
    public final static String EVENT_TYPE_PARAM = "et";
    public final static String USER_PARAM = "user";
    public final static String FROM_PARAM = "from";
    public final static String TO_PARAM = "to";
    public final static String SHOW = "show"; // all, watched, follows
    public final static String SHOW_ALL = "all";
    public final static String SHOW_WATCHED = "watched";
    public final static String SHOW_FOLLOWS = "follows";


    private final Serializer subjectSerializer;

    public EventResource(@Context GraphDatabaseService database) {
        super(database, SystemEvent.class);

        // Subjects are only serialized to depth 1 for efficiency...
        subjectSerializer = new Serializer.Builder(graph).withDepth(1).build();

    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getEvent(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List actions.
     *
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listEvents(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            final @QueryParam(EVENT_TYPE_PARAM) List<String> eventTypes,
            final @QueryParam(ITEM_TYPE_PARAM) List<String> itemTypes,
            final @QueryParam(ITEM_ID_PARAM) List<String> itemIds,
            final @QueryParam(USER_PARAM) List<String> users,
            final @QueryParam(FROM_PARAM) String from,
            final @QueryParam(TO_PARAM) String to)
            throws ItemNotFound, BadRequester {
        Query<SystemEvent> query = new Query<SystemEvent>(graph, SystemEvent.class);
        ActionManager am = new ActionManager(graph);

        Iterable<SystemEvent> list = query
                .list(am.getLatestGlobalEvents(), getRequesterUserProfile());

        // Add optional filters for event type, item type, and user...
        GremlinPipeline<SystemEvent,SystemEvent> pipe = new GremlinPipeline<SystemEvent, SystemEvent>(
                list);

        pipe = filterEvents(pipe, eventTypes, itemTypes, users, from, to, itemIds);

        return streamingList(pipe.range(offset, offset + (limit - 1)));
    }

    /**
     * List actions.
     * 
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/forUser/{userId:.+}")
    public StreamingOutput listEventsForUser(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            final @QueryParam(EVENT_TYPE_PARAM) List<String> eventTypes,
            final @QueryParam(ITEM_TYPE_PARAM) List<String> itemTypes,
            final @QueryParam(ITEM_ID_PARAM) List<String> itemIds,
            final @QueryParam(USER_PARAM) List<String> users,
            final @QueryParam(FROM_PARAM) String from,
            final @QueryParam(TO_PARAM) String to,
            final @QueryParam(SHOW) @DefaultValue(SHOW_ALL) String show)
            throws ItemNotFound, BadRequester {

        UserProfile user = getCurrentUser();
        Query<SystemEvent> query = new Query<SystemEvent>(graph, SystemEvent.class);
        ActionManager am = new ActionManager(graph);

        final PipeFunction<Vertex, Boolean> aclFilterTest = aclManager.getAclFilterFunction(user);

        // Set IDs to items this user is watching...
        final List<String> watching = Lists.newArrayList();
        for (Watchable item : user.getWatching()) {
            watching.add(item.getId());
        }

        final List<String> following = Lists.newArrayList();
        for (UserProfile other : user.getFollowing()) {
            following.add(other.getId());
        }

        Iterable<SystemEvent> list = query
                .list(am.getLatestGlobalEvents(), getRequesterUserProfile());

        // Add optional filters for event type, item type, and user...
        GremlinPipeline<SystemEvent,SystemEvent> pipe = new GremlinPipeline<SystemEvent, SystemEvent>(
                list);

        // Add additional generic filters
        pipe = filterEvents(pipe, eventTypes, itemTypes, users, from, to, itemIds);

        // Filter out those we're not watching, or are actioned
        // by users we're not following...
        pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
            @Override
            public Boolean compute(SystemEvent event) {
                if (show.equals(SHOW_ALL) || show.equals(SHOW_WATCHED)) {
                    for (AccessibleEntity e : event.getSubjects()) {
                        if (watching.contains(e.getId())) {
                            return true;
                        }
                    }
                }
                if (show.equals(SHOW_ALL) || show.equals(SHOW_FOLLOWS)) {
                    Actioner actioner = event.getActioner();
                    if (actioner != null && following.contains(actioner.getId())) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Filter items accessible to this user... hide the
        // event if any subjects or the scope are inaccessible
        // to the user.
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

        return streamingList(pipe.range(offset, offset + (limit - 1)));
    }

    private GremlinPipeline<SystemEvent, SystemEvent> filterEvents(
            GremlinPipeline<SystemEvent, SystemEvent> pipe,
            final List<String> eventTypes, final List<String> itemTypes,
            final List<String> users, final String from, final String to,
            final List<String> itemIds) {

        if (!eventTypes.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    return eventTypes.contains(event.getEventType());
                }
            });
        }

        if (!itemIds.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    for (AccessibleEntity e : event.getSubjects()) {
                        if (itemIds.contains(e.getId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        if (!itemTypes.isEmpty()) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    for (AccessibleEntity e : event.getSubjects()) {
                        if (itemTypes.contains(e.getType())) {
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
        if (from != null) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    String timestamp = event.getTimestamp();
                    return from.compareTo(timestamp) <= 0;
                }
            });
        }

        if (to != null) {
            pipe = pipe.filter(new PipeFunction<SystemEvent, Boolean>() {
                @Override
                public Boolean compute(SystemEvent event) {
                    String timestamp = event.getTimestamp();
                    return to.compareTo(timestamp) >= 0;
                }
            });
        }
        return pipe;
    }

    /**
     * Lookup and page the history for a given item.
     * 
     * @param id The event id
     * @param offset The history offset
     * @param limit The max number of items
     * @return A list of events
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/subjects")
    public StreamingOutput pageSubjectsForEvent(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        SystemEvent event = views.detail(id, user);
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        // NB: Taking a pragmatic decision here to only stream the first
        // level of the subject's tree.
        return streamingPage(query.page(event.getSubjects(), user),
                subjectSerializer.withCache());
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id The event id
     * @param offset The history offset
     * @param limit The max number of items
     * @return A list of events
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public StreamingOutput pageEventsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AccessibleEntity item = new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(id, user);
        Query<SystemEvent> query = new Query<SystemEvent>(graph, SystemEvent.class)
                .setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(item.getHistory(), user));
    }

    /**
     * Lookup and page the versions for a given item.
     *
     * @param id The event id
     * @param offset The history offset
     * @param limit The max number of items
     * @return A list of versions
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/versions/{id:.+}")
    public StreamingOutput pageVersionsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AccessibleEntity item = new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(id, user);
        Query<Version> query = new Query<Version>(graph, Version.class)
                .setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(item.getAllPriorVersions(), user));
    }
}
