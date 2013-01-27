package eu.ehri.extension;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.views.impl.AclViews;

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
    public Response getContentType(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
        new AclViews(graph).revokePermissionGrant(manager.getFrame(id,
                EntityClass.PERMISSION_GRANT, PermissionGrant.class),
                getRequesterUserProfile());
        return Response.status(Status.OK).build();
    }
}
