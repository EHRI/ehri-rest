package eu.ehri.extension;

import java.util.List;
import java.util.Set;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Sets;
import eu.ehri.project.exceptions.SerializationError;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.views.AclViews;

/**
 * Provides a RESTfull(ish) interface for setting PermissionTarget perms.
 */
@Path("access")
public class AccessResource extends
        AbstractAccessibleEntityResource<AccessibleEntity> {

    public AccessResource(@Context GraphDatabaseService database) {
        super(database, AccessibleEntity.class);
    }

    /**
     * Set the accessors who are able to view an item. If no accessors
     * are set, the item is globally readable.
     *
     * @param id
     * @param accessorIds
     * @return the updated object
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id:[^/]+}")
    public Response setVisibility(@PathParam("id") String id,
            @QueryParam(ACCESSOR_PARAM) List<String> accessorIds)
            throws PermissionDenied, ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Set<Accessor> accessors = extractAccessors(accessorIds);
            AclViews acl = new AclViews(graph);
            acl.setAccessors(item, accessors, getRequesterUserProfile());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK)
                    .entity((getSerializer().vertexFrameToJson(item)).getBytes())
                    .build();
        } finally {
            cleanupTransaction();
        }

    }

    /**
     * Get a set of Accessors from the given IDs.
     *
     * @param accessorIds
     * @return
     * @throws ItemNotFound
     */
    private Set<Accessor> extractAccessors(Iterable<String> accessorIds) throws ItemNotFound {
        Set<Accessor> accs = Sets.newHashSet();
        for (String at : accessorIds) {
            accs.add(manager.getFrame(at, Accessor.class));
        }
        return accs;
    }
}
