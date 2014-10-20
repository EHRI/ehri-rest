package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.*;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful(ish) interface for setting PermissionTarget perms.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.PERMISSION)
public class PermissionsResource extends AbstractRestResource {

    private final AclManager aclManager;
    private final AclViews aclViews;

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
        aclManager = new AclManager(graph);
        aclViews = new AclViews(graph);
    }

    /**
     * Get the cache control that should be applied to permission
     * data.
     *
     * @return A cache control object
     */
    public static CacheControl getCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(AbstractRestResource.ITEM_CACHE_TIME);
        return cacheControl;
    }

    /**
     * Get a list of permission grants for the given user
     *
     * @param id The user's id
     * @return A list of permission grants for the user
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list/{id:.+}")
    public Response listPermissionGrants(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(user.getPermissionGrants(), accessor,
                        PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return A list of grants for this item
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForItem/{id:.+}")
    public Response listPermissionGrantsForItem(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(target.getPermissionGrants(), accessor, PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return A list of grants for the given scope
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForScope/{id:.+}")
    public Response listPermissionGrantsForScope(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        return streamingPage(getQuery(AccessibleEntity.class)
                .page(scope.getPermissionGrants(), accessor,
                        PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return The current user's global permissions
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InheritedGlobalPermissionSet getGlobalMatrix() throws PermissionDenied,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        return getGlobalMatrix(getRequesterUserProfile().getId());
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId The user ID
     * @return The user's global permissions
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public InheritedGlobalPermissionSet getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        return aclManager.getInheritedGlobalPermissions(accessor);
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId  The user ID
     * @param globals The permission matrix data
     * @return The new permissions
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public InheritedGlobalPermissionSet setGlobalMatrix(
            @PathParam("userId") String userId,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            Accessor grantee = getRequesterUserProfile();
            InheritedGlobalPermissionSet newPerms
                    = aclViews
                    .setGlobalPermissionMatrix(accessor, globals, grantee);
            graph.getBaseGraph().commit();
            return newPerms;
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Get the permission matrix for a given user on the given entity.
     *
     * @param userId The user's ID
     * @param id     The item id
     * @return The user's permissions for that item
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet getEntityMatrix(
            @PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
        AclManager acl = aclManager.withScope(entity.getPermissionScope());
        return acl.getInheritedItemPermissions(entity, accessor);
    }

    /**
     * Get the user's permissions for a given scope.
     *
     * @param userId The user's permissions
     * @param id     The scope ID
     * @return The matrix for the given scope
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        AclManager acl = aclManager.withScope(scope);
        return acl.getInheritedGlobalPermissions(accessor);
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId  the user
     * @param id      the scope id
     * @param globals the serialized permission list
     * @return The new permission matrix
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet setScopedPermissions(
            @PathParam("userId") String userId,
            @PathParam("id") String id,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = aclViews.withScope(scope);
            acl.setGlobalPermissionMatrix(accessor, globals, grantee);
            graph.getBaseGraph().commit();
            return getScopedMatrix(userId, id);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Set a user's permissions on a given item.
     *
     * @param id        the item id
     * @param userId    the user id
     * @param itemPerms the serialized permission list
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet setItemPermissions(
            @PathParam("userId") String userId,
            @PathParam("id") String id,
            ItemPermissionSet itemPerms) throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor grantee = getRequesterUserProfile();
            aclViews.setItemPermissions(item, accessor, itemPerms.asSet(), grantee);
            graph.getBaseGraph().commit();
            return aclManager.getInheritedItemPermissions(item, accessor);
        } finally {
            cleanupTransaction();
        }
    }
}