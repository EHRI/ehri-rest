/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.extension;

import eu.ehri.project.acl.*;
import eu.ehri.project.core.Tx;
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
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list/{id:.+}")
    public Response listPermissionGrants(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = manager.getFrame(id, Accessor.class);
            Accessor accessor = getRequesterUserProfile();
            return streamingPage(getQuery(AccessibleEntity.class)
                    .page(user.getPermissionGrants(), accessor,
                            PermissionGrant.class), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List all the permission grants that relate specifically to this item.
     *
     * @return A list of grants for this item
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForItem/{id:.+}")
    public Response listPermissionGrantsForItem(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            PermissionGrantTarget target = manager.getFrame(id,
                    PermissionGrantTarget.class);
            Accessor accessor = getRequesterUserProfile();
            return streamingPage(getQuery(AccessibleEntity.class)
                    .page(target.getPermissionGrants(), accessor, PermissionGrant.class), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List all the permission grants that relate specifically to this scope.
     *
     * @return A list of grants for the given scope
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/listForScope/{id:.+}")
    public Response listPermissionGrantsForScope(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor accessor = getRequesterUserProfile();
            return streamingPage(getQuery(AccessibleEntity.class)
                    .page(scope.getPermissionGrants(), accessor,
                            PermissionGrant.class), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return The current user's global permissions
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InheritedGlobalPermissionSet getGlobalMatrix() throws PermissionDenied,
            ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            InheritedGlobalPermissionSet matrix = getGlobalMatrix(getRequesterUserProfile().getId());
            tx.success();
            return matrix;
        }
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
    @Path("{userId:.+}")
    public InheritedGlobalPermissionSet getGlobalMatrix(@PathParam("userId") String userId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            InheritedGlobalPermissionSet set = aclManager.getInheritedGlobalPermissions(accessor);
            tx.success();
            return set;
        }
    }

    /**
     * Set a user's global permission matrix.
     *
     * @param userId  The user ID
     * @param globals The permission matrix data
     * @return The new permissions
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}")
    public InheritedGlobalPermissionSet setGlobalMatrix(
            @PathParam("userId") String userId,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            Accessor grantee = getRequesterUserProfile();
            InheritedGlobalPermissionSet newPerms
                    = aclViews.setGlobalPermissionMatrix(accessor, globals, grantee);
            tx.success();
            return newPerms;
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
    @Path("{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet getEntityMatrix(
            @PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            AccessibleEntity entity = manager.getFrame(id, AccessibleEntity.class);
            AclManager acl = aclManager.withScope(entity.getPermissionScope());
            InheritedItemPermissionSet set = acl.getInheritedItemPermissions(entity, accessor);
            tx.success();
            return set;
        }
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
    @Path("{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet getScopedMatrix(@PathParam("userId") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            AclManager acl = aclManager.withScope(scope);
            InheritedGlobalPermissionSet set = acl.getInheritedGlobalPermissions(accessor);
            tx.success();
            return set;
        }
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
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/scope/{id:.+}")
    public InheritedGlobalPermissionSet setScopedPermissions(
            @PathParam("userId") String userId,
            @PathParam("id") String id,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            PermissionScope scope = manager.getFrame(id, PermissionScope.class);
            Accessor grantee = getRequesterUserProfile();
            AclViews acl = aclViews.withScope(scope);
            InheritedGlobalPermissionSet matrix = acl.setGlobalPermissionMatrix(accessor, globals, grantee);
            tx.success();
            return matrix;
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
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userId:.+}/{id:.+}")
    public InheritedItemPermissionSet setItemPermissions(
            @PathParam("userId") String userId,
            @PathParam("id") String id,
            ItemPermissionSet itemPerms) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getFrame(userId, Accessor.class);
            AccessibleEntity item = manager.getFrame(id, AccessibleEntity.class);
            Accessor grantee = getRequesterUserProfile();
            aclViews.setItemPermissions(item, accessor, itemPerms.asSet(), grantee);
            InheritedItemPermissionSet set = aclManager.getInheritedItemPermissions(item, accessor);
            tx.success();
            return set;
        }
    }
}