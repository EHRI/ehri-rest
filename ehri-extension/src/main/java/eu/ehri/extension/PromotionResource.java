package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.views.PromotionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Endpoints for promoting and demoting items.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path("promote")
public class PromotionResource extends AbstractRestResource {

    public PromotionResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Promote an item.
     *
     * @param id ID of item to promote.
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Path("/{id:.+}")
    public Response promoteItem(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        try {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            PromotionViews pv = new PromotionViews(graph);
            pv.promoteItem(item, currentUser);
            graph.getBaseGraph().commit();
            return Response.ok().location(getItemUri(item)).build();
        } catch (PromotionViews.NotPromotableError e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                    .entity(e.getMessage()).build();
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Demote an item (removing the user's promotion).
     *
     * @param id ID of item to remove
     * @return 200 response
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}")
    public Response demoteItem(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError, BadRequester {
        try {
            Promotable item = manager.getFrame(id, Promotable.class);
            UserProfile currentUser = getCurrentUser();
            PromotionViews pv = new PromotionViews(graph);
            pv.demoteItem(item, currentUser);
            graph.getBaseGraph().commit();
            return Response.ok().location(getItemUri(item)).build();
        } finally {
            cleanupTransaction();
        }
    }
}
