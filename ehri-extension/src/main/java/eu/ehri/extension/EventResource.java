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

import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull interface for the Event class. Note: Event instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path(Entities.SYSTEM_EVENT)
public class EventResource extends AbstractAccessibleEntityResource<SystemEvent> {

    private final Serializer subjectSerializer;

    public EventResource(@Context GraphDatabaseService database) {
        super(database, SystemEvent.class);

        // Subjects are only serialized to depth 1 for efficiency...
        subjectSerializer = new Serializer.Builder(graph).withDepth(1).build();

    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getAction(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List actions.
     * 
     * @param offset
     * @param limit
     * @param order
     * @param filters
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listEvents(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Query<SystemEvent> query = new Query<SystemEvent>(graph,
                SystemEvent.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        ActionManager am = new ActionManager(graph);
        return streamingList(query.list(am.getLatestGlobalEvents(),
                getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageEvents(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Query<SystemEvent> query = new Query<SystemEvent>(graph,
                SystemEvent.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        ActionManager am = new ActionManager(graph);
        return streamingPage(query.page(am.getLatestGlobalEvents(),
                getRequesterUserProfile()));
    }

    /**
     * Lookup and page the history for a given item.
     * 
     * @param id
     * @param offset
     * @param limit
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
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
        SystemEvent event = views.detail(manager.getFrame(id, cls), user);
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
     * @param id
     * @param offset
     * @param limit
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
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
                AccessibleEntity.class).detail(
                manager.getFrame(id, AccessibleEntity.class), user);
        Query<SystemEvent> query = new Query<SystemEvent>(graph, SystemEvent.class)
                .setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(item.getHistory(), user));
    }
}
