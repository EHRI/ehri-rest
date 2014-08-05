package eu.ehri.extension;

import java.io.IOException;
import java.util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import eu.ehri.project.acl.*;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.AclViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTful(ish) interface for setting PermissionTarget perms.
 *
 * @author http://github.com/mikesname
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

    private CacheControl getCacheControl() {
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list/{id:.+}")
    public StreamingOutput listPermissionGrants(
            @PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor user = manager.getFrame(id, Accessor.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(getQ.list(user.getPermissionGrants(), accessor,
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForItem/{id:.+}")
    public StreamingOutput listPermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return A list of grants
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pageForItem/{id:.+}")
    public StreamingOutput pagePermissionGrantsForItem(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionGrantTarget target = manager.getFrame(id,
                PermissionGrantTarget.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(target.getPermissionGrants(), accessor,
                PermissionGrant.class));
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/listForScope/{id:.+}")
    public StreamingOutput listPermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return The grants for the given scope
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pageForScope/{id:.+}")
    public StreamingOutput pagePermissionGrantsForScope(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws PermissionDenied, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        Accessor accessor = getRequesterUserProfile();
        Query<AccessibleEntity> query = new Query<AccessibleEntity>(graph,
                AccessibleEntity.class).setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(scope.getPermissionGrants(), accessor,
                PermissionGrant.class));
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return The current user's global permissions
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/foo")
    public InheritedGlobalPermissionSet getGlobalMatrix() throws PermissionDenied, IOException,
            ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = getRequesterUserProfile();
        return getGlobalMatrix(accessor.getId());
    }

    /**
     * Get the global permission matrix for the given accessor.
     *
     * @param userId The user ID
     * @return The user's global permissions
     * @throws PermissionDenied
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public InheritedGlobalPermissionSet getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, IOException, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);

        return aclManager
                .getInheritedGlobalPermissions(accessor);
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId The user ID
     * @param perms  The global permission set.
     * @return The new permissions
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}")
    public InheritedGlobalPermissionSet setGlobalMatrix(@PathParam("userId") String userId,
            GlobalPermissionSet perms) throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        try {
            InheritedGlobalPermissionSet newPerms
                    = aclViews
                    .setGlobalPermissionMatrix(accessor, perms, grantee);
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
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet getEntityMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, IOException,
            ItemNotFound {
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
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     * @throws DeserializationError
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound,
            IOException, DeserializationError {
        graph.getBaseGraph().checkNotInTransaction();
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        PermissionScope scope = manager.getFrame(id, PermissionScope.class);
        return aclManager.withScope(scope)
                .getInheritedGlobalPermissions(accessor);
    }

    /**
     * Set a user's permissions on a content type with a given scope.
     *
     * @param userId the user
     * @param id     the scope id
     * @param perms  The global permission set
     * @return The new permission matrix
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet setScopedPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id,
            GlobalPermissionSet perms) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();

        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            aclViews.withScope(scope)
                    .setGlobalPermissionMatrix(accessor, perms, grantee);
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
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet setItemPermissions(@PathParam("userId") String userId,
            @PathParam("id") String id, ItemPermissionSet itemPerms) throws PermissionDenied,
            IOException, ItemNotFound, DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();

        try {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = new AclViews(graph);
            acl.setItemPermissions(item, accessor, itemPerms.asSet(), grantee);
            graph.getBaseGraph().commit();

            return aclManager.getInheritedItemPermissions(manager.getFrame(id,
                    AccessibleEntity.class), accessor);
        } finally {
            cleanupTransaction();
        }
    }
}