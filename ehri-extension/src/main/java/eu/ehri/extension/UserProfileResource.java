package eu.ehri.extension;

import com.google.common.collect.Sets;
import eu.ehri.extension.base.*;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
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
 * Provides a RESTful interface for the UserProfile.
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
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id)
            throws AccessDenied, ItemNotFound, BadRequester {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    @Override
    public long count() throws BadRequester {
        return countItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    @Override
    public Response list() throws BadRequester {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createUserProfile(Bundle bundle,
    		@QueryParam(GROUP_PARAM) List<String> groupIds,
    		@QueryParam(ACCESSOR_PARAM) List<String> accessors) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        final UserProfile currentUser = getCurrentUser();
        try {
            final Set<Group> groups = Sets.newHashSet();
            for (String groupId : groupIds) {
                groups.add(manager.getFrame(groupId, Group.class));
            }
            return createItem(bundle, accessors, new Handler<UserProfile>() {
                @Override
                public void process(UserProfile userProfile) throws PermissionDenied {
                    for (Group group : groups) {
                        aclViews.addAccessorToGroup(group, userProfile, currentUser);
                    }
                }
            });
        } catch (ItemNotFound e) {
            graph.getBaseGraph().rollback();
            throw new DeserializationError("User or group given as accessor not found: " + e.getValue());
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response update(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return updateItem(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return updateItem(id, bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return deleteItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWERS)
    public Response listFollowers(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(UserProfile.class)
                .page(user.getFollowers(), accessor));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWING)
    public Response listFollowing(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(UserProfile.class)
                .page(user.getFollowing(), accessor));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_FOLLOWING + "/{otherId:.+}")
    public boolean isFollowing(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return user.isFollowing(
                manager.getFrame(otherId, UserProfile.class));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_FOLLOWER + "/{otherId:.+}")
    public boolean isFollower(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return user.isFollower(manager.getFrame(otherId, UserProfile.class));
    }

    @POST
    @Path("{userId:.+}/" + FOLLOWING)
    public Response followUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addFollowing(manager.getFrame(id, UserProfile.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + FOLLOWING)
    public Response unfollowUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.removeFollowing(manager.getFrame(id, UserProfile.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + BLOCKED)
    public Response listBlocked(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(UserProfile.class).page(user.getBlocked(), accessor));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + IS_BLOCKING + "/{otherId:.+}")
    public boolean isBlocking(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return user.isBlocking(manager.getFrame(otherId, UserProfile.class));
    }

    @POST
    @Path("{userId:.+}/" + BLOCKED)
    public Response blockUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addBlocked(manager.getFrame(id, UserProfile.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + BLOCKED)
    public Response unblockUserProfile(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.removeBlocked(manager.getFrame(id, UserProfile.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + WATCHING)
    public Response listWatching(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(Watchable.class)
                .page(user.getWatching(), accessor));
    }

    @POST
    @Path("{userId:.+}/" + WATCHING)
    public Response watchItem(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id : otherIds) {
                user.addWatching(manager.getFrame(id, Watchable.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + WATCHING)
    public Response unwatchItem(
            @PathParam("userId") String userId,
            @QueryParam(ID_PARAM) List<String> otherIds)
            throws BadRequester, PermissionDenied, ItemNotFound {
        try {
            Accessor accessor = getRequesterUserProfile();
            UserProfile user = views.detail(userId, accessor);
            for (String id :  otherIds) {
                user.removeWatching(manager.getFrame(id, Watchable.class));
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_WATCHING + "/{otherId:.+}")
    public boolean isWatching(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return user.isWatching(manager.getFrame(otherId, Watchable.class));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.ANNOTATION)
    public Response listAnnotations(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(Annotation.class)
                .page(user.getAnnotations(), accessor));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.LINK)
    public Response pageLinks(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(Link.class).page(user.getLinks(), accessor));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.VIRTUAL_UNIT)
    public Response pageVirtualUnits(@PathParam("userId") String userId)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return streamingPage(getQuery(VirtualUnit.class)
                .page(user.getVirtualUnits(), accessor));
    }
}
