/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.ws;

import eu.ehri.project.ws.base.AbstractResource;
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
 * Web service resource for setting and reading permissions.
 */
@Path(PermissionsResource.ENDPOINT)
public class PermissionsResource extends AbstractResource {

    public static final String ENDPOINT = "permissions";

    public PermissionsResource(@Context GraphDatabaseService database) {
        super(database);
    }

    /**
     * Get the cache control that should be applied to permission
     * data.
     *
     * @return A cache control object
     */
    public static CacheControl getCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(AbstractResource.ITEM_CACHE_TIME);
        return cacheControl;
    }

    /**
     * Get the global permission matrix for the user making the request, based
     * on the Authorization header.
     *
     * @return The current user's global permissions
     * @throws ItemNotFound if there is not corresponding user in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InheritedGlobalPermissionSet getGlobalMatrix() throws ItemNotFound {
        try (final Tx tx = beginTx()) {
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
     * @throws ItemNotFound if the user or group does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}")
    public InheritedGlobalPermissionSet getGlobalMatrix(@PathParam("userOrGroup") String userId)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            InheritedGlobalPermissionSet set = api()
                    .aclManager()
                    .getInheritedGlobalPermissions(accessor);
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
     * @throws ItemNotFound if the user or group does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}")
    public InheritedGlobalPermissionSet setGlobalMatrix(
            @PathParam("userOrGroup") String userId,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            InheritedGlobalPermissionSet newPerms
                    = api().acl().setGlobalPermissionMatrix(accessor, globals);
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
     * @throws ItemNotFound if the user or group does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}/item/{id:[^/]+}")
    public InheritedItemPermissionSet getEntityMatrix(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible entity = api().get(id, Accessible.class);
            AclManager acl = api().withScope(entity.getPermissionScope()).aclManager();
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
     * @return an inherited permission set
     * @throws ItemNotFound if the user or group does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}/item/{id:[^/]+}")
    public InheritedItemPermissionSet setItemPermissions(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id,
            ItemPermissionSet itemPerms) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible item = api().get(id, Accessible.class);
            InheritedItemPermissionSet set = api().acl()
                    .setItemPermissions(item, accessor, itemPerms.asSet());
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
     * @throws ItemNotFound if the user or group does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}/scope/{id:[^/]+}")
    public InheritedGlobalPermissionSet getScopedMatrix(@PathParam("userOrGroup") String userId,
            @PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible scope = api().get(id, Accessible.class);
            InheritedGlobalPermissionSet set = api()
                    .withScope(scope.as(PermissionScope.class))
                    .aclManager()
                    .getInheritedGlobalPermissions(accessor);
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
     * @throws ItemNotFound if the user or group does not exist
     * @throws PermissionDenied if the user cannot perform the action
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}/scope/{id:[^/]+}")
    public InheritedGlobalPermissionSet setScopedPermissions(
            @PathParam("userOrGroup") String userId,
            @PathParam("id") String id,
            GlobalPermissionSet globals) throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Accessor accessor = manager.getEntity(userId, Accessor.class);
            Accessible scope = api().get(id, Accessible.class);
            InheritedGlobalPermissionSet matrix = api()
                    .withScope(scope.as(PermissionScope.class))
                    .acl()
                    .setGlobalPermissionMatrix(accessor, globals);
            tx.success();
            return matrix;
        }
    }

    /**
     * Get a list of permission grants for the given user
     *
     * @param id the user's id
     * @return a list of permission grants for the user
     * @throws ItemNotFound if the user or group does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{userOrGroup:[^/]+}/permission-grants")
    public Response listPermissionGrants(@PathParam("userOrGroup") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, Accessor.class);
            Response page = streamingPage(() -> getQuery()
                    .page(manager.getEntityUnchecked(id, Accessor.class)
                            .getPermissionGrants(), PermissionGrant.class));
            tx.success();
            return page;
        }
    }
}