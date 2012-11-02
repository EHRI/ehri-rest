package eu.ehri.extension;

import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.views.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path("/access")
public class AccessResource extends EhriNeo4jFramedResource<AccessibleEntity> {

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    @POST
    @Path("/{id:\\d+}")
    public Response setVisibility(@PathParam("id") long id,
            @FormParam("accessor") Set<Long> accessors)
            throws PermissionDenied {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            AclViews<AccessibleEntity> acl = new AclViews<AccessibleEntity>(graph,
                    cls);
            acl.setAccessors(id, accessors, getRequesterUserProfileId());
            tx.success();
            return Response.status(Status.OK).build();
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }        
    }

}
