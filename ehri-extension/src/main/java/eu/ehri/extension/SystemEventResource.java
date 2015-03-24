/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.extension;

import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.EventViews;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for the Event model. Note: Event instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.SYSTEM_EVENT)
public class SystemEventResource extends AbstractAccessibleEntityResource<SystemEvent>
        implements GetResource {

    public final static String ITEM_TYPE_PARAM = "type";
    public final static String ITEM_ID_PARAM = "item";
    public final static String EVENT_TYPE_PARAM = "et";
    public final static String USER_PARAM = "user";
    public final static String FROM_PARAM = "from";
    public final static String TO_PARAM = "to";
    public final static String SHOW = "show"; // watched, follows


    private final Serializer subjectSerializer;

    public SystemEventResource(@Context GraphDatabaseService database) {
        super(database, SystemEvent.class);

        // Subjects are only serialized to depth 1 for efficiency...
        subjectSerializer = new Serializer.Builder(graph).withDepth(1).build();

    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return getItem(id);
    }

    /**
     * List global events. Standard list parameters for paging apply.
     *
     * @param eventTypes Filter events by type
     * @param itemTypes  Filter events based on the item type of their subjects
     * @param itemIds    Filter events pertaining to specific item IDs
     * @param users      Filter events based on the user IDs they involve
     * @param from       Exclude events prior to this data
     * @param to         Exclude events after this data
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
            throws BadRequester {

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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/byUser/{userId:.+}")
    public Response listEventsByUser(
            final @PathParam("userId") String userId,
            final @QueryParam(EVENT_TYPE_PARAM) List<EventTypes> eventTypes,
            final @QueryParam(ITEM_TYPE_PARAM) List<String> itemTypes,
            final @QueryParam(ITEM_ID_PARAM) List<String> itemIds,
            final @QueryParam(USER_PARAM) List<String> users,
            final @QueryParam(FROM_PARAM) String from,
            final @QueryParam(TO_PARAM) String to)
            throws ItemNotFound, BadRequester {

        Accessor user = getRequesterUserProfile();
        UserProfile byUser = manager.getFrame(userId, UserProfile.class);
        EventViews eventViews = new EventViews(graph)
                .withEventTypes(eventTypes.toArray(new EventTypes[eventTypes.size()]))
                .withEntityTypes(itemTypes.toArray(new String[itemTypes.size()]))
                .withIds(itemIds.toArray(new String[itemIds.size()]))
                .withUsers(users.toArray(new String[users.size()]))
                .from(from)
                .to(to);

        return streamingList(eventViews.listByUser(getQuery(cls), byUser, user));
    }

    /**
     * List actions that are relevant to a given user based on
     * the other users that they follow and the items they watch.
     *
     * @param userId     The user's ID
     * @param eventTypes Filter events by type
     * @param itemTypes  Filter events based on the item type of their subjects
     * @param itemIds    Filter events pertaining to specific item IDs
     * @param users      Filter events based on the user IDs they involve
     * @param from       Exclude events prior to this data
     * @param to         Exclude events after this data
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
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(event.getSubjects(), user), subjectSerializer.withCache());
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
