package eu.ehri.extension;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;

/**
 * Provides a RESTfull interface for the Group class.
 */
@Path(Entities.GROUP)
public class GroupResource extends AbstractAccessibleEntityResource<Group> {

    public GroupResource(@Context GraphDatabaseService database) {
        super(database, Group.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getGroup(@PathParam("id") long id) throws PermissionDenied,
            BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getGroup(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listGroups(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return list(offset, limit);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/page")
    public StreamingOutput pageGroups(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return page(offset, limit);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        return create(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGroup(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response updateGroup(@PathParam("id") String id, String json)
            throws PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    /**
     * Add an accessor to a group.
     * 
     * @param id
     * @param atype
     * @param aid
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response addAccessor(@PathParam("id") String id,
            @PathParam("atype") String atype, @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound, BadRequester {
        // FIXME: Add permission checks for this!!!
        // TODO: Check existing membership?
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Group group = manager.getFrame(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getFrame(aid, Accessor.class);
            group.addMember(accessor);
            
            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(getRequesterUserProfile().asVertex(), Actioner.class),
                    "Added accessor to group").addSubjects(group);
            
            tx.success();
            
            // TODO: Is there anything worth return here except OK?
            return Response.status(Status.OK).build();
        } finally {
            tx.finish();
        }
    }
    
    /**
     * Remove an accessor from a group.
     * 
     * @param id
     * @param atype
     * @param aid
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:[^/]+}/{aid:.+}")
    public Response removeAccessor(@PathParam("id") String id,
            @PathParam("aid") String aid)
            throws PermissionDenied, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            // FIXME: Add permission checks for this!!!
            Group group = manager.getFrame(id, EntityClass.GROUP, Group.class);
            Accessor accessor = manager.getFrame(aid, Accessor.class);
            group.removeMember(accessor);
            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(getRequesterUserProfile().asVertex(), Actioner.class),
                    "Removed accessor from group").addSubjects(group);
            
            tx.success();
            
            // TODO: Is there anything worth return here except OK?
            return Response.status(Status.OK).build();
        } finally {
            tx.finish();
        }
    }    
    
    /**
     * Delete a group with the given graph ID.
     * 
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id}")
    public Response deleteGroup(@PathParam("id") long id)
            throws PermissionDenied, ValidationError, ItemNotFound,
            BadRequester {
        return delete(id);
    }

    /**
     * Delete a group with the given identifier string.
     * 
     * @param id
     * @return
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteGroup(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
}
