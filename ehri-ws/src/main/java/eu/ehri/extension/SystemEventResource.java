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

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.EventViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for the Event model. Note: Event instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 * <p>
 * The following query parameters apply to all actions in this
 * resource to apply filtering to the event streams.
 * </p>
 * <dl>
 * <dt>eventTypes</dt><dd>Filter events by type</dd>
 * <dt>itemTypes</dt><dd>Filter events based on the item type of their subjects</dd>
 * <dt>itemIds</dt><dd>Filter events pertaining to specific item IDs</dd>
 * <dt>users</dt><dd>Filter events based on the user IDs they involve</dd>
 * <dt>from</dt><dd>Exclude events prior to this date (ISO 8601 format)</dd>
 * <dt>to</dt><dd>Exclude events after this date (ISO 8601 format)</dd>
 * </dl>
 * <p>
 * Additionally the aggregate* end-points accept an <code>aggregation</code>
 * parameter that groups sequential events according to one of two different
 * strategies:
 * <dl>
 * <dt>user</dt>
 * <dd>Groups sequential events of all types that are initiated by the same actioner</dd>
 * <dt>strict</dt>
 * <dd>
 * Groups sequential events that:
 * <ul>
 * <li>have the same type</li>
 * <li>have the same actioner</li>
 * <li>have the same subjects</li>
 * <li>have the same scope</li>
 * <li>have the same log message</li>
 * </ul>
 * </dd>
 * </dl>
 * <p>
 * Additionally, aggregation can be disabled by using <code>aggregation=off</code>.
 * <p>
 * Standard paging parameters apply to all end-points.
 */
@Path(Entities.SYSTEM_EVENT)
public class SystemEventResource extends AbstractAccessibleResource<SystemEvent>
        implements GetResource {

    public final static String ITEM_TYPE_PARAM = "type";
    public final static String ITEM_ID_PARAM = "item";
    public final static String EVENT_TYPE_PARAM = "et";
    public final static String USER_PARAM = "user";
    public final static String FROM_PARAM = "from";
    public final static String TO_PARAM = "to";
    public final static String SHOW_PARAM = "show"; // watched, follows
    public final static String AGGREGATION_PARAM = "aggregation";


    private final Serializer subjectSerializer;

    public SystemEventResource(@Context GraphDatabaseService database) {
        super(database, SystemEvent.class);

        // Subjects are only serialized to depth 1 for efficiency...
        subjectSerializer = new Serializer.Builder(graph).withDepth(1).build();

    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    /**
     * List aggregated global events. Standard list parameters for paging apply.
     *
     * @param aggregation The manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    "user".
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventViews.Aggregation aggregation) {

        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            EventViews eventViews = getEventViewsBuilder()
                    .withAggregation(aggregation)
                    .build();
            return streamingListOfLists(eventViews.aggregate(user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Fetch a list of a user's actions.
     *
     * @param userId the user's ID
     * @return a list of event ranges
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("byUser/{userId:.+}")
    public Response listUserActions(
            @PathParam("userId") String userId) throws ItemNotFound {

        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = manager.getEntity(userId, UserProfile.class);
            EventViews eventViews = getEventViewsBuilder().build();
            return streamingList(eventViews.listByUser(user, accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Fetch an aggregate list of a user's actions.
     *
     * @param userId      the user's ID
     * @param aggregation The manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    <b>strict</b>.
     * @return a list of event ranges
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aggregateByUser/{userId:.+}")
    public Response aggregateUserActions(
            @PathParam("userId") String userId,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("strict") EventViews.Aggregation aggregation) throws ItemNotFound {

        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = manager.getEntity(userId, UserProfile.class);
            EventViews eventViews = getEventViewsBuilder()
                    .withAggregation(aggregation)
                    .build();
            return streamingListOfLists(eventViews.aggregateByUser(user, accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List actions that are relevant to a given user based on
     * the other users that they follow and the items they watch.
     *
     * @param userId the user's ID
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("forUser/{userId:.+}")
    public Response listEventsForUser(
            @PathParam("userId") String userId) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            UserProfile asUser = manager.getEntity(userId, UserProfile.class);
            EventViews eventViews = getEventViewsBuilder().build();
            return streamingList(eventViews.listAsUser(asUser, user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Aggregate actions that are relevant to a given user based on
     * the other users that they follow and the items they watch.
     *
     * @param userId      the user's ID
     * @param aggregation The manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    "user".
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aggregateForUser/{userId:.+}")
    public Response aggregateEventsForUser(
            @PathParam("userId") String userId,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventViews.Aggregation aggregation) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            UserProfile asUser = manager.getEntity(userId, UserProfile.class);
            EventViews eventViews = getEventViewsBuilder()
                    .withAggregation(aggregation)
                    .build();
            return streamingListOfLists(eventViews.aggregateAsUser(asUser, user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id The event id
     * @return A list of events
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("for/{id:.+}")
    public Response pageEventsForItem(
            @PathParam("id") String id)
            throws ItemNotFound, AccessDenied {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            Accessible item = views
                    .setClass(Accessible.class).detail(id, accessor);
            EventViews eventViews = getEventViewsBuilder().build();
            return streamingList(eventViews.listForItem(item, accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Lookup and page the history for a given item.
     *
     * @param id          The event id
     * @param aggregation the aggregation stategy
     * @return A list of events
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aggregateFor/{id:.+}")
    public Response aggregateEventsForItem(
            @PathParam("id") String id,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventViews.Aggregation aggregation)
            throws ItemNotFound, AccessDenied {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            Accessible item = views
                    .setClass(Accessible.class).detail(id, accessor);
            EventViews eventViews = getEventViewsBuilder()
                    .withAggregation(aggregation)
                    .build();

            return streamingListOfLists(eventViews.aggregateForItem(item, accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Fetch a page of subjects for a given event.
     *
     * @param id the event id
     * @return a list of subject items
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}/subjects")
    public Response pageSubjectsForEvent(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            SystemEvent event = views.detail(id, user);
            return streamingPage(getQuery(Accessible.class)
                    .page(event.getSubjects(), user), subjectSerializer.withCache(), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    // Helpers

    private EventViews.Builder getEventViewsBuilder() {
        List<String> eventTypes = getStringListQueryParam(EVENT_TYPE_PARAM);
        List<String> entityClasses = getStringListQueryParam(ITEM_TYPE_PARAM);
        List<String> showTypes = getStringListQueryParam(SHOW_PARAM);
        List<String> fromStrings = getStringListQueryParam(FROM_PARAM);
        List<String> toStrings = getStringListQueryParam(TO_PARAM);
        List<String> users = getStringListQueryParam(USER_PARAM);
        List<String> ids = getStringListQueryParam(ITEM_ID_PARAM);
        return new EventViews.Builder(graph)
                .withRange(getOffset(), getLimit())
                .withEventTypes(eventTypes.toArray(new String[eventTypes.size()]))
                .withEntityTypes(entityClasses.toArray(new String[entityClasses.size()]))
                .from(fromStrings.isEmpty() ? null : fromStrings.get(0))
                .to(toStrings.isEmpty() ? null : toStrings.get(0))
                .withUsers(users.toArray(new String[users.size()]))
                .withIds(ids.toArray(new String[ids.size()]))
                .withShowType(showTypes.toArray(new String[showTypes.size()]));
    }

    private int getOffset() {
        return getIntQueryParam(OFFSET_PARAM, 0);
    }

    private int getLimit() {
        return getIntQueryParam(LIMIT_PARAM, DEFAULT_LIST_LIMIT);
    }
}
