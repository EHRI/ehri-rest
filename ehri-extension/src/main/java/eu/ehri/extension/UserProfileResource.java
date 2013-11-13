package eu.ehri.extension;

import static eu.ehri.extension.RestHelpers.produceErrorMessageJson;

import java.net.URI;
import java.util.List;

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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.views.Query;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;

/**
 * Provides a RESTfull interface for the UserProfile.
 */
@Path(Entities.USER_PROFILE)
public class UserProfileResource extends AbstractAccessibleEntityResource<UserProfile> {

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
    		@QueryParam(GROUP_PARAM) List<String> groups, 
    		@QueryParam(ACCESSOR_PARAM) List<String> accessors) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        return createWithGroups(json, groups, accessors);
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
    @Path("/followers")
    public StreamingOutput listFollowers(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        UserProfile user = getCurrentUser();
        final Iterable<UserProfile> list = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).list(user.getFollowing(), user);
        return streamingList(list);
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/follow/{userId:.+}")
    public Response followsUserProfile(@PathParam("userId") String userId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        UserProfile user = getCurrentUser();
        return booleanResponse(user.isFollowing(
                manager.getFrame(userId, UserProfile.class)));
    }

    @POST
    @Path("/follow/{userId:.+}")
    public Response followUserProfile(@PathParam("userId") String userId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        UserProfile user = getCurrentUser();
        try {
            user.addFollowing(manager.getFrame(userId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }

    @POST
    @Path("/unfollow/{userId:.+}")
    public Response unfollowUserProfile(@PathParam("userId") String userId)
            throws BadRequester, PermissionDenied, ItemNotFound {
        UserProfile user = getCurrentUser();
        try {
            user.removeFollowing(manager.getFrame(userId, UserProfile.class));
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        }  finally {
            cleanupTransaction();
        }
    }


    /*** helpers ***/

    /*
     * This code is similar to the create method of AbstractAccessibleEntityResource 
     * but also allows specifying groups to which the new userProfile will be added. 
     * 
     * TODO needs to be refactored, but for now ... its not clear if it will be used elsewhere;  
     * maybe on group's?
     */
    private Response createWithGroups(String json, List<String> groupIds, List<String> accessorIds)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, BadRequester {

        Accessor user = getRequesterUserProfile();
        Bundle entityBundle = Bundle.fromString(json);
        try {
            UserProfile entity = views.create(entityBundle, user,getLogMessage());
            // TODO: Move elsewhere
            new AclManager(graph).setAccessors(entity,
                    getAccessors(accessorIds, user));

            String jsonStr = serializer.vertexFrameToJson(entity);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI docUri = ub.path(entity.getId()).build();
            
            // add to the groups
            for (String groupId: groupIds) {
            	Group group = manager.getFrame(groupId, EntityClass.GROUP, Group.class);
            	// TODO checking the creator has permission to add the user to the group 
            	// see GroupResource.addMember
            	group.addMember(entity);
                // note that group addition is now logged separately
                new ActionManager(graph).logEvent(
                        graph.frame(entity.asVertex(), AccessibleEntity.class),
                        graph.frame(getRequesterUserProfile().asVertex(), Actioner.class),
                        EventTypes.addGroup).addSubjects(group);
            }
            
            graph.getBaseGraph().commit();
            return Response.status(Status.CREATED).location(docUri)
                    .entity((jsonStr).getBytes()).build();
        } catch (ItemNotFound e) {
            graph.getBaseGraph().rollback();
            return Response.status(Status.BAD_REQUEST)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }
}
