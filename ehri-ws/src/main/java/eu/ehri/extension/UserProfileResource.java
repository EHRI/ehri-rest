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

import com.google.common.collect.Sets;
import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.UserProfilesApi;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

/**
 * Provides a web service interface for the UserProfile.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.USER_PROFILE)
public class UserProfileResource extends AbstractAccessibleResource<UserProfile>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String FOLLOWING = "following";
    public static final String FOLLOWERS = "followers";
    public static final String IS_FOLLOWING = "is-following";
    public static final String IS_FOLLOWER = "is-follower";
    public static final String WATCHING = "watching";
    public static final String IS_WATCHING = "is-watching";
    public static final String BLOCKED = "blocked";
    public static final String IS_BLOCKING = "is-blocking";
    public static final String ACTIONS = "actions";
    public static final String EVENTS = "events";
    public static final String VIRTUAL_UNITS = "virtual-units";

    public UserProfileResource(@Context GraphDatabaseService database) {
        super(database, UserProfile.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response list() {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUserProfile(Bundle bundle,
            @QueryParam(GROUP_PARAM) List<String> groupIds,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors) throws PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Api.Acl acl = api().acl();
            final Set<Group> groups = Sets.newHashSet();
            for (String groupId : groupIds) {
                groups.add(manager.getEntity(groupId, Group.class));
            }
            Response item = createItem(bundle, accessors, userProfile -> {
                for (Group group : groups) {
                    acl.addAccessorToGroup(group, userProfile);
                }
            });
            tx.success();
            return item;
        } catch (ItemNotFound e) {
            throw new DeserializationError("User or group given as accessor not found: " + e.getValue());
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + FOLLOWERS)
    public Response listFollowers(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getFollowers(), UserProfile.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + FOLLOWING)
    public Response listFollowing(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getFollowing(), UserProfile.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + IS_FOLLOWING + "/{otherId:[^/]+}")
    public boolean isFollowing(
            @PathParam("id") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            boolean following = user.isFollowing(
                    manager.getEntity(otherId, UserProfile.class));
            tx.success();
            return following;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + IS_FOLLOWER + "/{otherId:[^/]+}")
    public boolean isFollower(
            @PathParam("id") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            boolean follower = user.isFollower(manager.getEntity(otherId, UserProfile.class));
            tx.success();
            return follower;
        }
    }

    @POST
    @Path("{id:[^/]+}/" + FOLLOWING)
    public void followUserProfile(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().addFollowers(userId, otherIds);
            tx.success();
        }
    }

    @DELETE
    @Path("{id:[^/]+}/" + FOLLOWING)
    public void unfollowUserProfile(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().removeFollowers(userId, otherIds);
            tx.success();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + BLOCKED)
    public Response listBlocked(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getBlocked(), UserProfile.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + IS_BLOCKING + "/{otherId:[^/]+}")
    public boolean isBlocking(
            @PathParam("id") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            boolean blocking = user.isBlocking(manager.getEntity(otherId, UserProfile.class));
            tx.success();
            return blocking;
        }
    }

    @POST
    @Path("{id:[^/]+}/" + BLOCKED)
    public void blockUserProfile(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().addBlocked(userId, otherIds);
            tx.success();
        }
    }

    @DELETE
    @Path("{id:[^/]+}/" + BLOCKED)
    public void unblockUserProfile(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().removeBlocked(userId, otherIds);
            tx.success();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + WATCHING)
    public Response listWatching(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getWatching(), Watchable.class));
            tx.success();
            return response;
        }
    }

    @POST
    @Path("{id:[^/]+}/" + WATCHING)
    public void watchItem(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().addWatching(userId, otherIds);
            tx.success();
        }
    }

    @DELETE
    @Path("{id:[^/]+}/" + WATCHING)
    public void unwatchItem(
            @PathParam("id") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            userProfilesApi().removeWatching(userId, otherIds);
            tx.success();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + IS_WATCHING + "/{otherId:[^/]+}")
    public boolean isWatching(
            @PathParam("id") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            boolean watching = user.isWatching(manager.getEntity(otherId, Watchable.class));
            tx.success();
            return watching;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + GenericResource.ANNOTATIONS)
    public Response listAnnotations(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getAnnotations(), Annotation.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + GenericResource.LINKS)
    public Response pageLinks(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getLinks(), Link.class));
            tx.success();
            return response;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + VIRTUAL_UNITS)
    public Response pageVirtualUnits(@PathParam("id") String userId) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = api().detail(userId, cls);
            Response response = streamingPage(getQuery()
                    .page(user.getVirtualUnits(), VirtualUnit.class));
            tx.success();
            return response;
        }
    }

    /**
     * Fetch an aggregate list of a user's actions.
     *
     * @param userId      the user's ID
     * @param aggregation the manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    <b>strict</b>.
     * @return a list of event ranges
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + ACTIONS)
    public Response aggregateUserActions(
            @PathParam("id") String userId,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("strict") EventsApi.Aggregation aggregation)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile user = manager.getEntity(userId, UserProfile.class);
            EventsApi eventsApi = getEventsApi()
                    .withAggregation(aggregation);
            Response response = streamingListOfLists(eventsApi.aggregateUserActions(user));
            tx.success();
            return response;
        }
    }

    /**
     * Aggregate actions that are relevant to a given user based on
     * the other users that they follow and the items they watch.
     *
     * @param userId      the user's ID
     * @param aggregation the manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    <b>user</b>.
     * @return a list of event ranges
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/" + EVENTS)
    public Response aggregateEventsForUser(
            @PathParam("id") String userId,
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventsApi.Aggregation aggregation)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            UserProfile asUser = manager.getEntity(userId, UserProfile.class);
            EventsApi eventsApi = getEventsApi()
                    .withAggregation(aggregation);
            Response response = streamingListOfLists(eventsApi.aggregateAsUser(asUser));
            tx.success();
            return response;
        }
    }

    // Helpers

    private UserProfilesApi userProfilesApi() {
        // Logging on these events is currently not enabled.
        return api().enableLogging(false).userProfiles();
    }
}
