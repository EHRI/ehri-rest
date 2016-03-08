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

import eu.ehri.extension.base.AbstractRestResource;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import eu.ehri.project.acl.InheritedItemPermissionSet;
import eu.ehri.project.acl.ItemPermissionSet;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful(ish) interface for setting PermissionTarget perms.
 */
@Path(PermissionsResource.ENDPOINT)
public class PermissionsResource extends AbstractRestResource {

    public static final String ENDPOINT = "permissions";

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
    @Path("{userOrGroup:[^/]+}")
    public InheritedGlobalPermissionSet getGlobalMatrix(@PathParam("userOrGroup") String userId)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
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
    @Path("{userOrGroup:[^/]+}")
    public InheritedGlobalPermissionSet setGlobalMatrix(
            @PathParam("userOrGroup") String userId,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
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
    @Path("{userOrGroup:[^/]+}/item/{id:[^/]+}")
    public InheritedItemPermissionSet getEntityMatrix(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible entity = manager.getEntity(id, Accessible.class);
            AclManager acl = aclManager.withScope(entity.getPermissionScope());
            InheritedItemPermissionSet set = acl.getInheritedItemPermissions(entity, accessor);
            tx.success();
            return set;
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
    @Path("{userOrGroup:[^/]+}/item/{id:[^/]+}")
    public InheritedItemPermissionSet setItemPermissions(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id,
            ItemPermissionSet itemPerms) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible item = manager.getEntity(id, Accessible.class);
            Accessor grantee = getRequesterUserProfile();
            InheritedItemPermissionSet set = aclViews
                    .setItemPermissions(item, accessor, itemPerms.asSet(), grantee);
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
    @Path("{userOrGroup:[^/]+}/scope/{id:[^/]+}")
    public InheritedGlobalPermissionSet getScopedMatrix(@PathParam("userOrGroup") String userId,
            @PathParam("id") String id) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            PermissionScope scope = manager.getEntity(id, PermissionScope.class);
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
    @Path("{userOrGroup:[^/]+}/scope/{id:[^/]+}")
    public InheritedGlobalPermissionSet setScopedPermissions(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            PermissionScope scope = manager.getEntity(id, PermissionScope.class);
            InheritedGlobalPermissionSet matrix = aclViews
                    .withScope(scope)
                    .setGlobalPermissionMatrix(accessor, globals, getRequesterUserProfile());
            tx.success();
            return matrix;
        }
    }

    /**
     * Get a list of permission grants for the given user
     *
     * @param id the user's id
     * @return a list of permission grants for the user
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{userOrGroup:[^/]+}/" + GenericResource.PERMISSION_GRANTS)
    public Response listPermissionGrants(@PathParam("userOrGroup") String id) throws ItemNotFound {
        final Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = manager.getEntity(id, Accessor.class);
            return streamingPage(getQuery(PermissionGrant.class)
                    .page(user.getPermissionGrants(), getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }
}