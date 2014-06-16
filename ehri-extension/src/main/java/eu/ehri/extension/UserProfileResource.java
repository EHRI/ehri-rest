package eu.ehri.extension;

import java.util.List;
import java.util.Set;

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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Sets;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.views.Query;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;

import static eu.ehri.extension.RestHelpers.produceErrorMessageJson;

/**
 * Provides a RESTful interface for the UserProfile.
 */
@Path(Entities.USER_PROFILE)
public class UserProfileResource extends AbstractAccessibleEntityResource<UserProfile> {

    public static final String FOLLOW = "follow";
    public static final String FOLLOWING = "following";
    public static final String FOLLOWERS = "followers";
    public static final String IS_FOLLOWING = "isFollowing";
    public static final String IS_FOLLOWER = "isFollower";
    public static final String WATCH = "watch";
    public static final String WATCHING = "watching";
    public static final String IS_WATCHING = "isWatching";
    public static final String BLOCK = "block";
    public static final String BLOCKED = "blocked";
    public static final String IS_BLOCKING = "isBlocking";

    public UserProfileResource(@Context GraphDatabaseService database) {
        super(database, UserProfile.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getUserProfile(@PathParam("id") String id)
            throws AccessDenied, ItemNotFound, PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listUserProfiles(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countUserProfiles(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageUserProfiles(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createUserProfile(String json,
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
            return create(json, accessors, new PostCreateHandler<UserProfile>() {
                @Override
                public void process(UserProfile userProfile) throws PermissionDenied {
                    for (Group group: groups) {
                        aclViews.addAccessorToGroup(group, userProfile, currentUser);
                    }
                }
            });
        } catch (ItemNotFound e) {
            graph.getBaseGraph().rollback();
            return Response.status(Status.BAD_REQUEST)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateUserProfile(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateUserProfile(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteUserProfile(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWERS)
    public StreamingOutput listFollowers(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Iterable<UserProfile> list = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).list(user.getFollowers(), accessor);
        return streamingList(list);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWERS + "/page")
    public StreamingOutput pageFollowers(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<UserProfile> page = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getFollowers(), accessor);
        return streamingPage(page);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWING)
    public StreamingOutput listFollowing(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Iterable<UserProfile> list = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).list(user.getFollowing(), accessor);
        return streamingList(list);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + FOLLOWING + "/page")
    public StreamingOutput pageFollowing(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<UserProfile> page = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getFollowing(), accessor);
        return streamingPage(page);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_FOLLOWING + "/{otherId:.+}")
    public Response isFollowing(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return booleanResponse(user.isFollowing(
                manager.getFrame(otherId, UserProfile.class)));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_FOLLOWER + "/{otherId:.+}")
    public Response isFollower(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return booleanResponse(user
                .isFollower(manager.getFrame(otherId, UserProfile.class)));
    }

    @POST
    @Path("{userId:.+}/" + FOLLOW + "/{otherId:.+}")
    public Response followUserProfile(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.addFollowing(manager.getFrame(otherId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + FOLLOW + "/{otherId:.+}")
    public Response unfollowUserProfile(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.removeFollowing(manager.getFrame(otherId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + BLOCKED)
    public StreamingOutput listBlocked(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Iterable<UserProfile> list = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).list(user.getBlocked(), accessor);
        return streamingList(list);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + BLOCKED + "/page")
    public StreamingOutput pageBlocked(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<UserProfile> page = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getBlocked(), accessor);
        return streamingPage(page);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_BLOCKING + "/{otherId:.+}")
    public Response isBlocking(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return booleanResponse(user.isBlocking(
                manager.getFrame(otherId, UserProfile.class)));
    }

    @POST
    @Path("{userId:.+}/" + BLOCK + "/{otherId:.+}")
    public Response blockUserProfile(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.addBlocked(manager.getFrame(otherId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + BLOCK + "/{otherId:.+}")
    public Response unblockUserProfile(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.removeBlocked(manager.getFrame(otherId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + WATCHING)
    public StreamingOutput listWatching(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Iterable<Watchable> list = new Query<Watchable>(graph,
                Watchable.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).list(user.getWatching(), accessor);
        return streamingList(list);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + WATCHING + "/page")
    public StreamingOutput pageWatching(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<Watchable> page = new Query<Watchable>(graph,
                Watchable.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getWatching(), accessor);
        return streamingPage(page);
    }

    @POST
    @Path("{userId:.+}/" + WATCH + "/{otherId:.+}")
    public Response watchItem(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.addWatching(manager.getFrame(otherId, Watchable.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @DELETE
    @Path("{userId:.+}/" + WATCH + "/{otherId:.+}")
    public Response unwatchItem(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        try {
            user.removeWatching(manager.getFrame(otherId, Watchable.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/" + IS_WATCHING + "/{otherId:.+}")
    public Response isWatching(
            @PathParam("userId") String userId,
            @PathParam("otherId") String otherId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        return booleanResponse(user
                .isWatching(manager.getFrame(otherId, Watchable.class)));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.ANNOTATION + "/page")
    public StreamingOutput pageAnnotations(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<Annotation> page = new Query<Annotation>(graph,
                Annotation.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getAnnotations(), accessor);
        return streamingPage(page);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userId:.+}/" + Entities.LINK + "/page")
    public StreamingOutput pageLinks(
            @PathParam("userId") String userId,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        UserProfile user = views.detail(userId, accessor);
        final Query.Page<Link> page = new Query<Link>(graph,
                Link.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(user.getLinks(), accessor);
        return streamingPage(page);
    }
}
