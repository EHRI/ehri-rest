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
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Set;

/**
 * Provides a web service interface for the UserProfile.
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 * @author Mike Bryant (https://github.com/mikesname)
 */
@Path(Entities.USER_PROFILE)
public class UserProfileResource extends AbstractAccessibleEntityResource<UserProfile>
        implements GetResource, ListResource, UpdateResource, DeleteResource {

    public static final String FOLLOWING = "following";
    public static final String FOLLOWERS = "followers";
    public static final String IS_FOLLOWING = "isFollowing";
    public static final String IS_FOLLOWER = "isFollower";
    public static final String WATCHING = "watching";
    public static final String IS_WATCHING = "isWatching";
    public static final String BLOCKED = "blocked";
    public static final String IS_BLOCKING = "isBlocking";

    public UserProfileResource(@Context GraphDatabaseService database) {
        super(database, UserProfile.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response list() {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createUserProfile(Bundle bundle,
                                      @QueryParam(GROUP_PARAM) List<String> groupIds,
                                      @QueryParam(ACCESSOR_PARAM) List<String> accessors) throws PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            final UserProfile currentUser = getCurrentUser();
            final Set<Group> groups = Sets.newHashSet();
            for (String groupId : groupIds) {
                groups.add(manager.getFrame(groupId, Group.class));
            }
            Response item = createItem(bundle, accessors, new Handler<UserProfile>() {
                @Override
                public void process(UserProfile userProfile) throws PermissionDenied {
                    for (Group group : groups) {
                        aclViews.addAccessorToGroup(group, userProfile, currentUser);
                    }
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = deleteItem(id);
            tx.success();
            return item;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWERS)
    public Response listFollowers(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(UserProfile.class)
                    .page(user.getFollowers(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWING)
    public Response listFollowing(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(UserProfile.class)
                    .page(user.getFollowing(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_FOLLOWING + "/{otherId:.+}")
    public boolean isFollowing(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            boolean following = user.isFollowing(
                    manager.getFrame(otherId, UserProfile.class));
            tx.success();
            return following;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_FOLLOWER + "/{otherId:.+}")
    public boolean isFollower(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            boolean follower = user.isFollower(manager.getFrame(otherId, UserProfile.class));
            tx.success();
            return follower;
        }
    }

    @POST
    @Path("{userId:.+}/" + FOLLOWING)
    public Response followUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addFollowing(manager.getFrame(id, UserProfile.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + FOLLOWING)
    public Response unfollowUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.removeFollowing(manager.getFrame(id, UserProfile.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + BLOCKED)
    public Response listBlocked(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(UserProfile.class)
                    .page(user.getBlocked(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_BLOCKING + "/{otherId:.+}")
    public boolean isBlocking(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            boolean blocking = user.isBlocking(manager.getFrame(otherId, UserProfile.class));
            tx.success();
            return blocking;
        }
    }

    @POST
    @Path("{userId:.+}/" + BLOCKED)
    public Response blockUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addBlocked(manager.getFrame(id, UserProfile.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + BLOCKED)
    public Response unblockUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.removeBlocked(manager.getFrame(id, UserProfile.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + WATCHING)
    public Response listWatching(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(Watchable.class)
                    .page(user.getWatching(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @POST
    @Path("{userId:.+}/" + WATCHING)
    public Response watchItem(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addWatching(manager.getFrame(id, Watchable.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + WATCHING)
    public Response unwatchItem(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.removeWatching(manager.getFrame(id, Watchable.class));
            }
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_WATCHING + "/{otherId:.+}")
    public boolean isWatching(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            boolean watching = user.isWatching(manager.getFrame(otherId, Watchable.class));
            tx.success();
            return watching;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.ANNOTATION)
    public Response listAnnotations(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(Annotation.class)
                    .page(user.getAnnotations(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.LINK)
    public Response pageLinks(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(Link.class)
                    .page(user.getLinks(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.VIRTUAL_UNIT)
    public Response pageVirtualUnits(@PathParam("userId") String userId) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            return streamingPage(getQuery(VirtualUnit.class)
                    .page(user.getVirtualUnits(), accessor), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }
}
