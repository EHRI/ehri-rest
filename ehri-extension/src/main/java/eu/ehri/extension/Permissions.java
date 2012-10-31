package eu.ehri.extension;

import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.utils.AnnotationUtils;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.views.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path(EntityTypes.PERMISSION)
public class Permissions extends AbstractRestResource {

    public Permissions(@Context GraphDatabaseService database) {
        super(database);
    }

    @POST
    @Path("/set/{id:\\d+}")
    public Response setPermissionMatrix(@PathParam("id") long id,
            @FormParam("ctype") List<Long> ctypes,
            @FormParam("perm") List<Long> perms,
            @FormParam("status") List<Boolean> status) throws PermissionDenied {

        // Sanity check the matrix is the right shape...
        if (!(ctypes.size() == perms.size() && status.size() == perms.size())) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            // remove all existing permission grants
            Accessor accessor = graph.getVertex(id, Accessor.class);
            Accessor grantee = graph.getVertex(getRequesterUserProfileId(),
                    Accessor.class);
            AclManager acl = new AclManager(graph);
            AclViews<ContentType> aclview = new AclViews<ContentType>(graph,
                    ContentType.class);
            aclview.checkPermission(getRequesterUserProfileId(), PermissionTypes.GRANT);        

            // Remove all accessor's existing permissions on content types...
            for (PermissionGrant grant : accessor.getPermissionGrants()) {
                if (AnnotationUtils.hasFramedInterface(grant.getTarget(),
                        ContentType.class)) {
                    acl.revokePermissions(accessor, grant.getTarget(),
                            grant.getPermission());
                }
            }

            for (int i = 0; i < ctypes.size(); i++) {
                ContentType ctype = graph.getVertex(ctypes.get(i),
                        ContentType.class);
                Permission perm = graph.getVertex(perms.get(i),
                        Permission.class);
                PermissionGrant grant = acl.grantPermissions(accessor, ctype,
                        perm);
                grant.setGrantee(grantee);
            }

            // Log the action...
            new ActionManager(graph).createAction(
                    graph.frame(accessor.asVertex(), AccessibleEntity.class),
                    graph.frame(grantee.asVertex(), Actioner.class),
                    "Updated permissions");

            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }

        return Response.status(Status.OK).build();
    }
}
