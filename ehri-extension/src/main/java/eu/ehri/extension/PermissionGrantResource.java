package eu.ehri.extension;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.ehri.project.exceptions.SerializationError;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.views.AclViews;

/**
 * Provides a RESTfull interface for the PermissionGrant class.
 * 
 * FIXME: PermissionGrant is not currently an AccessibleEntity so
 * handling it is complicated. We need to re-architect the REST views
 * to handle more than just the initially-envisaged scenarios.
 */
@Path(Entities.PERMISSION_GRANT)
public class PermissionGrantResource extends AbstractRestResource {

    public PermissionGrantResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Fetch a given permission grant.
     *
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getPermissionGrant(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester, SerializationError {
        // TODO: Should we add ACL checks here???
        PermissionGrant grant = manager.getFrame(id,
                EntityClass.PERMISSION_GRANT, PermissionGrant.class);
        return Response.status(Status.OK).entity(
                getSerializer().vertexFrameToJson(grant).getBytes()).build();
    }
    /**
     * Revoke a particular permission grant.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response revokePermissionGrant(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
        try {
            new AclViews(graph).revokePermissionGrant(manager.getFrame(id,
                    EntityClass.PERMISSION_GRANT, PermissionGrant.class),
                    getRequesterUserProfile());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } finally {
            cleanupTransaction();
        }
    }
}
