package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.EventViews;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
    public final static String SHOW = "show"; // watched, follows


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
    public Response listEvents(
            final @QueryParam(EVENT_TYPE_PARAM) List<EventTypes> eventTypes,
            final @QueryParam(ITEM_TYPE_PARAM) List<String> itemTypes,
            final @QueryParam(ITEM_ID_PARAM) List<String> itemIds,
            final @QueryParam(USER_PARAM) List<String> users,
            final @QueryParam(FROM_PARAM) String from,
            final @QueryParam(TO_PARAM) String to)
            throws ItemNotFound, BadRequester {

        Accessor user = getRequesterUserProfile();
        EventViews eventViews = new EventViews(graph)
                .withEventTypes(eventTypes.toArray(new EventTypes[eventTypes.size()]))
                .withEntityTypes(itemTypes.toArray(new String[itemTypes.size()]))
                .withIds(itemIds.toArray(new String[itemIds.size()]))
                .withUsers(users.toArray(new String[users.size()]))
                .from(from)
                .to(to);

        return streamingList(eventViews.list(getQuery(cls), user));
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
    public Response listEventsForUser(
            final @PathParam("userId") String userId,
            final @QueryParam(EVENT_TYPE_PARAM) List<EventTypes> eventTypes,
            final @QueryParam(ITEM_TYPE_PARAM) List<String> itemTypes,
            final @QueryParam(ITEM_ID_PARAM) List<String> itemIds,
            final @QueryParam(USER_PARAM) List<String> users,
            final @QueryParam(FROM_PARAM) String from,
            final @QueryParam(TO_PARAM) String to,
            final @QueryParam(SHOW) List<EventViews.ShowType> show)
            throws ItemNotFound, BadRequester {

        Accessor user = getRequesterUserProfile();
        UserProfile asUser = manager.getFrame(userId, UserProfile.class);
        EventViews eventViews = new EventViews(graph)
                .withShowType(show.toArray(new EventViews.ShowType[show.size()]))
                .withEventTypes(eventTypes.toArray(new EventTypes[eventTypes.size()]))
                .withEntityTypes(itemTypes.toArray(new String[itemTypes.size()]))
                .withIds(itemIds.toArray(new String[itemIds.size()]))
                .withUsers(users.toArray(new String[users.size()]))
                .from(from)
                .to(to);

        return streamingList(eventViews.listAsUser(getQuery(cls), asUser, user));
    }

    /**
     * Lookup and page the history for a given item.
     * 
     * @param id The event id
     * @return A list of events
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/subjects")
    public Response pageSubjectsForEvent(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        SystemEvent event = views.detail(id, user);
        // NB: Taking a pragmatic decision here to only stream the first
        // level of the subject's tree.
        return streamingPage(getQuery(AccessibleEntity.class)
                .setStream(true).page(event.getSubjects(), user),
                subjectSerializer.withCache());
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id The event id
     * @return A list of events
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/for/{id:.+}")
    public Response pageEventsForItem(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AccessibleEntity item = new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(id, user);
        return streamingPage(getQuery(cls).setStream(true)
                .page(item.getHistory(), user));
    }

    /**
     * Lookup and page the versions for a given item.
     *
     * @param id The event id
     * @return A list of versions
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/versions/{id:.+}")
    public Response pageVersionsForItem(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AccessibleEntity item = new LoggingCrudViews<AccessibleEntity>(graph,
                AccessibleEntity.class).detail(id, user);
        return streamingPage(getQuery(Version.class).setStream(true)
                .page(item.getAllPriorVersions(), user));
    }
}
