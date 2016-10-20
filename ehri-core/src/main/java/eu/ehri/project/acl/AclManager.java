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

package eu.ehri.project.acl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class for checking and asserting access and write permissions.
 */
public final class AclManager {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final Set<PermissionScope> scopes;

    // Lookups to convert between the enum and node representations
    // of content and permission types.
    private final LoadingCache<PermissionType, Permission> enumPermissionMap = CacheBuilder.newBuilder()
            .build(new CacheLoader<PermissionType, Permission>() {
                @Override
                public Permission load(PermissionType permissionType) throws Exception {
                    return manager.getEntity(permissionType.getName(), Permission.class);
                }
            });
    private final LoadingCache<ContentTypes, ContentType> enumContentTypeMap = CacheBuilder.newBuilder()
            .build(new CacheLoader<ContentTypes, ContentType>() {
                @Override
                public ContentType load(ContentTypes contentTypes) throws Exception {
                    return manager.getEntity(contentTypes.getName(), ContentType.class);
                }
            });
    private final LoadingCache<Permission, PermissionType> permissionEnumMap = CacheBuilder.newBuilder()
            .build(new CacheLoader<Permission, PermissionType>() {
                @Override
                public PermissionType load(Permission permission) throws Exception {
                    return PermissionType.withName(permission.getId());
                }
            });
    private final LoadingCache<ContentType, ContentTypes> contentTypeEnumMap = CacheBuilder.newBuilder()
            .build(new CacheLoader<ContentType, ContentTypes>() {
                @Override
                public ContentTypes load(ContentType contentType) throws Exception {
                    return ContentTypes.withName(contentType.getId());
                }
            });

    /**
     * Scoped constructor.
     *
     * @param graph The framed graph
     * @param scope The ACL scope
     */
    public AclManager(FramedGraph<?> graph, PermissionScope scope) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.scope = Optional.ofNullable(scope).orElse(SystemScope.getInstance());
        this.scopes = getAllScopes();
    }

    /**
     * Constructor.
     *
     * @param graph The framed graph
     */
    public AclManager(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     *
     * @param accessor The user/group
     * @return User belongs to the admin group
     */
    public static boolean belongsToAdmin(Accessor accessor) {
        if (accessor.isAdmin()) {
            return true;
        }
        for (Accessor parent : accessor.getParents()) {
            if (belongsToAdmin(parent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     *
     * @param accessor The user/group
     * @return User is an anonymous accessor
     */
    public static boolean isAnonymous(Accessor accessor) {
        Preconditions.checkNotNull(accessor, "NULL accessor given.");
        return accessor instanceof AnonymousAccessor
                || accessor.getId().equals(
                Group.ANONYMOUS_GROUP_IDENTIFIER);
    }

    /**
     * Determine if a user can access an entity.
     *
     * @param entity   The item
     * @param accessor The user/group
     * @return Whether or not the given accessor can access the entity
     */
    public boolean canAccess(Accessible entity, Accessor accessor) {
        Preconditions.checkNotNull(entity, "Entity is null");
        Preconditions.checkNotNull(accessor, "Accessor is null");
        return getAclFilterFunction(accessor).compute(entity.asVertex());
    }

    /**
     * Revoke an accessor's access to an entity.
     *
     * @param entity   The item
     * @param accessor A user/group from whom to revoke access
     */
    public void removeAccessControl(Accessible entity, Accessor accessor) {
        entity.removeAccessor(accessor);
    }

    /**
     * Set access control on an entity to several accessors.
     *
     * @param entity    The item
     * @param accessors A set of users/groups who can access the item
     */
    public void setAccessors(Accessible entity,
            Collection<Accessor> accessors) {
        Set<Accessor> accessorVertices = Sets.newHashSet(accessors);
        Set<Accessor> remove = Sets.newHashSet();
        for (Accessor accessor : entity.getAccessors()) {
            if (!accessorVertices.contains(accessor)) {
                remove.add(accessor);
            }
        }
        for (Accessor accessor : remove) {
            entity.removeAccessor(accessor);
        }
        for (Accessor accessor : accessors) {
            entity.addAccessor(accessor);
        }
    }

    /**
     * Get a list of permissions for a given accessor on a given entity, including
     * inherited permissions. Returns a map of accessor IDs against grant permissions.
     *
     * @param accessor The accessor
     * @return An inherited item permission set.
     */
    public InheritedItemPermissionSet getInheritedItemPermissions(
            Accessible entity, Accessor accessor) {
        InheritedItemPermissionSet.Builder builder
                = new InheritedItemPermissionSet
                .Builder(accessor.getId(), getItemPermissions(accessor, entity));
        for (Accessor parent : accessor.getAllParents()) {
            builder.withInheritedPermissions(parent.getId(), getItemPermissions(parent, entity));
        }
        return builder.build();
    }

    /**
     * Set the permissions for a particular user on the given item.
     *
     * @param item          The item
     * @param accessor      The user/group
     * @param permissionSet A set of permissions
     */
    public void setItemPermissions(Accessible item, Accessor accessor,
            Set<PermissionType> permissionSet) throws PermissionDenied {
        checkNoGrantOnAdminOrAnon(accessor);
        for (PermissionType t : PermissionType.values()) {
            if (permissionSet.contains(t)) {
                grantPermission(item, t, accessor);
            } else {
                revokePermission(item, t, accessor);
            }
        }
    }

    /**
     * Return a permission list for the given accessor and her inherited groups.
     *
     * @param accessor The user/group
     * @return List of permission maps for the given accessor and his group
     * parents.
     */
    public InheritedGlobalPermissionSet getInheritedGlobalPermissions(
            Accessor accessor) {
        InheritedGlobalPermissionSet.Builder builder
                = new InheritedGlobalPermissionSet
                .Builder(accessor.getId(), getGlobalPermissions(accessor));
        for (Accessor parent : accessor.getParents()) {
            builder.withInheritedPermissions(parent.getId(), getGlobalPermissions(parent));
        }
        return builder.build();
    }

    /**
     * Recursive helper function to ascend an accessor's groups and populate
     * their global permissions.
     *
     * @param accessor The user/group
     * @return Permission map for the given accessor
     */
    public GlobalPermissionSet getGlobalPermissions(Accessor accessor) {
        return belongsToAdmin(accessor)
                ? getAdminPermissions()
                : getAccessorPermissions(accessor);
    }

    /**
     * Set a matrix of global permissions for a given accessor.
     *
     * @param accessor The user/group
     * @param globals  global permission map
     */
    public void setPermissionMatrix(Accessor accessor, GlobalPermissionSet globals)
            throws PermissionDenied {
        checkNoGrantOnAdminOrAnon(accessor);
        Map<ContentTypes, Collection<PermissionType>> globalsMap = globals.asMap();

        //for (Entry<ContentTypes, ContentType> centry : enumContentTypeMap.entrySet()) {
        for (ContentTypes ct : ContentTypes.values()) {
            ContentType target = enumContentTypeMap.getUnchecked(ct);
            Collection<PermissionType> pset = globalsMap.containsKey(ct)
                    ? globalsMap.get(ct)
                    : Sets.<PermissionType>newHashSet();
            for (PermissionType perm : PermissionType.values()) {
                if (pset.contains(perm)) {
                    grantPermission(target, perm, accessor);
                } else {
                    revokePermission(target, perm, accessor);
                }
            }
        }
    }

    /**
     * Grant a user permissions to a content type.
     *
     * @param target   The grant target (content type or item)
     * @param permType The permission type
     * @param accessor The user/group
     * @return The permission grant given for this accessor and target
     */
    public PermissionGrant grantPermission(PermissionGrantTarget target,
            PermissionType permType, Accessor accessor) {
        assertNoGrantOnAdminOrAnon(accessor);
        // If we can find an existing grant, use that, otherwise create a new one.
        Optional<PermissionGrant> maybeGrant = findPermission(target, permType, accessor);
        if (maybeGrant.isPresent()) {
            return maybeGrant.get();
        } else {
            PermissionGrant grant = createPermissionGrant();
            accessor.addPermissionGrant(grant);
            grant.setPermission(vertexForPermission(permType));
            grant.addTarget(target);
            if (!isSystemScope()) {
                grant.setScope(scope);
            }
            return grant;
        }
    }

    /**
     * Revoke a particular permission on the given entity.
     *
     * @param entity   The item
     * @param permType The permission type
     * @param accessor The user/group
     */
    public void revokePermission(Accessible entity, PermissionType permType,
            Accessor accessor) {
        Optional<PermissionGrant> maybeGrant = findPermission(entity, permType, accessor);
        if (maybeGrant.isPresent()) {
            manager.deleteVertex(maybeGrant.get().asVertex());
        }
    }

    /**
     * Revoke a particular permission grant.
     *
     * @param grant The grant to revoke
     */
    public void revokePermissionGrant(PermissionGrant grant) {
        manager.deleteVertex(grant.asVertex());
    }

    /**
     * Build a gremlin filter function that passes through items that are
     * bona fide content types.
     *
     * @return A PipeFunction for filtering vertices that are content types.
     */
    public PipeFunction<Vertex, Boolean> getContentTypeFilterFunction() {
        final Set<String> typeStrings = Sets.newHashSet();
        for (ContentTypes ct : ContentTypes.values()) {
            typeStrings.add(ct.getName());
        }
        return v -> v != null && typeStrings.contains(manager.getType(v));
    }

    /**
     * Build a gremlin filter function that passes through items readable by a
     * given accessor.
     *
     * @param accessor The user/group
     * @return A PipeFunction for filtering a set of vertices as the given user
     */
    public static PipeFunction<Vertex, Boolean> getAclFilterFunction(Accessor accessor) {
        Preconditions.checkNotNull(accessor, "Accessor is null");
        if (belongsToAdmin(accessor)) {
            return noopFilterFunction();
        }

        final HashSet<Vertex> all = getAllAccessors(accessor);
        return v -> {
            Iterable<Vertex> verts = v.getVertices(Direction.OUT,
                    Ontology.IS_ACCESSIBLE_TO);
            // If there's no Access conditions, it's
            // read-only...
            if (!verts.iterator().hasNext()) {
                return true;
            }
            // If it's promoted it's publicly accessible
            if (isPromoted(v)) {
                return true;
            }
            // Otherwise, check relevant accessors...
            for (Vertex other : verts) {
                if (all.contains(other)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Check if a user has permission to perform an action on the given content type.
     *
     * @param contentType    The content type
     * @param permissionType The requested permission
     * @param accessor       The user
     * @return If the user has permission on the given content type within the current scope
     */
    public boolean hasPermission(ContentTypes contentType, PermissionType permissionType, Accessor accessor) {
        return hasPermission(contentType, permissionType, accessor, scopes);
    }

    /**
     * Check if a user has permission to perform an action on the given item.
     *
     * @param entity         The item
     * @param permissionType The requested permission
     * @param accessor       The user
     * @return If the user has the permission on the given item
     */
    public boolean hasPermission(Accessible entity, PermissionType permissionType, Accessor accessor) {
        // Get a list of our current context scopes, plus
        // the parent scopes of the item.
        Set<PermissionScope> allScopes = Sets.newHashSet(scopes);
        for (PermissionScope scope : entity.getPermissionScopes()) {
            allScopes.add(scope);
        }

        // Check if the user has content type permissions on this item, using
        // the parent scope of the item...
        ContentTypes contentType = getContentType(manager.getEntityClass(entity));
        if (hasPermission(contentType, permissionType, accessor, allScopes)) {
            return true;
        }

        // Otherwise, we have to check the item's permissions...
        return hasScopedPermission(entity, permissionType, accessor, allScopes);
    }

    // Helpers...

    /**
     * Set scope.
     *
     * @param scope The new permission scope
     * @return A new ACL Manager
     */
    public AclManager withScope(PermissionScope scope) {
        return new AclManager(graph, scope);
    }

    /**
     * Get scope.
     *
     * @return The permission scope.
     */
    public PermissionScope getScope() {
        return scope;
    }

    /**
     * Check for a content permission with a given set of scopes.
     *
     * @param contentType    The content type
     * @param permissionType The permission type
     * @param accessor       The user/group
     * @param scopes         The item scopes
     * @return Whether or not the user has permission
     */
    private boolean hasPermission(ContentTypes contentType, PermissionType permissionType, Accessor accessor,
            Collection<PermissionScope> scopes) {

        ContentType contentTypeNode = enumContentTypeMap.getUnchecked(contentType);
        // Check the user themselves...
        return belongsToAdmin(accessor)
                || hasScopedPermission(contentTypeNode, permissionType, accessor, scopes);
    }

    /**
     * Attempt to find a permission, searching the accessor's parent hierarchy.
     *
     * @param target         A target permission grant
     * @param permissionType The permission type
     * @param accessor       The user/group
     * @param scopes         A set of parent scopes
     * @return Whether or not the grant was found
     */
    private boolean hasScopedPermission(PermissionGrantTarget target,
            PermissionType permissionType, Accessor accessor,
            Collection<PermissionScope> scopes) {

        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            PermissionType grantPermissionType
                    = enumForPermission(grant.getPermission());

            // If it's not the permission type we want, skip it...
            if (!grantPermissionType.contains(permissionType)) {
                continue;
            }

            // Get the content type node and check it matches the grant target
            for (PermissionGrantTarget tg : grant.getTargets()) {
                if (!target.equals(tg)) {
                    continue;
                }

                // Now check the scope - if there is none we're good.
                if (grant.getScope() == null || scopes.contains(grant.getScope())) {
                    return true;
                }
            }
        }

        // If no joy, check if they inherit the permission...
        for (Accessor ancestor : accessor.getParents()) {
            if (hasScopedPermission(target, permissionType, ancestor, scopes)) {
                return true;
            }
        }

        // Default case, return false....
        return false;
    }

    /**
     * Get the permission type enum for a given node.
     */
    private Permission vertexForPermission(PermissionType perm) {
        return enumPermissionMap.getUnchecked(perm);
    }

    /**
     * Get the permission type enum for a given node.
     */
    private PermissionType enumForPermission(Permission perm) {
        return permissionEnumMap.getUnchecked(perm);
    }

    /**
     * Get a list of global permissions for a given accessor. Returns a map of
     * content types against the grant permissions.
     *
     * @param accessor The user/group
     * @return List of permission names for the given accessor on the given
     * target
     */
    private List<PermissionType> getItemPermissions(Accessor accessor,
            Accessible entity) {
        // If we're admin, add it regardless.
        if (belongsToAdmin(accessor)) {
            return ImmutableList.copyOf(PermissionType.values());
        } else {
            List<PermissionType> list = Lists.newArrayList();
            // Cache a set of permission scopes. This is the hierarchy on which
            // permissions are granted. For most items it will contain zero
            // entries and thus be pretty fast, but for deeply nested
            // documentary units there might be quite a few.
            HashSet<PermissionScope> scopes = Sets.newHashSet(entity.getPermissionScopes());
            PermissionGrantTarget target = entity.as(PermissionGrantTarget.class);

            for (PermissionGrant grant : accessor.getPermissionGrants()) {
                if (Iterables.contains(grant.getTargets(), target)) {
                    list.add(enumForPermission(grant.getPermission()));
                } else if (grant.getScope() != null && hasContentTypeTargets(grant)) {
                    // If there isn't a direct grant to the entity, search its
                    // parent scopes for an appropriate scoped permission
                    if (scopes.contains(grant.getScope())) {
                        list.add(enumForPermission(grant.getPermission()));
                    }
                }
            }
            return list;
        }
    }

    /**
     * Attempt to locate an existing grant with the same accessor, entity, and
     * permission, within the given scope.
     */
    private Optional<PermissionGrant> findPermission(PermissionGrantTarget entity,
            PermissionType permType, Accessor accessor) {

        PermissionGrantTarget target = entity.as(PermissionGrantTarget.class);
        Permission perm = enumPermissionMap.getUnchecked(permType);
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (isInScope(grant)
                    && Iterables.contains(grant.getTargets(), target)
                    && grant.getPermission().equals(perm)) {
                return Optional.of(grant);
            }
        }
        return Optional.empty();
    }

    private PermissionGrant createPermissionGrant() {
        try {
            Vertex vertex = manager.createVertex(
                    EntityClass.PERMISSION_GRANT.getIdGen()
                            .generateId(Lists.<String>newArrayList(), null),
                    EntityClass.PERMISSION_GRANT,
                    Maps.newHashMap());
            return graph.frame(vertex, PermissionGrant.class);
        } catch (IntegrityError e) {
            e.printStackTrace();
            throw new RuntimeException("Something very unlikely has occured because two" +
                    " supposedly-random numbers have collided. Trying again should fix this.");
        }
    }

    private void checkNoGrantOnAdminOrAnon(Accessor accessor)
            throws PermissionDenied {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        if (accessor.isAdmin() || accessor.isAnonymous()) {
            throw new PermissionDenied(
                    "Unable to grant or revoke permissions to system accounts.");
        }
    }

    private void assertNoGrantOnAdminOrAnon(Accessor accessor) {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        // This time throw  a runtime error.
        if (accessor.isAdmin() || accessor.isAnonymous()) {
            throw new RuntimeException(
                    "Unable to grant or revoke permissions to system accounts.");
        }
    }

    /**
     * For a given user, fetch a lookup of all the inherited accessors it
     * belongs to. NB: This returns a lookup of raw Vertices because it's
     * used by the a Gremlin filter function, which likewise operates
     * directly on vertices.
     *
     * @param accessor The user/group
     * @return A lookup of accessor vertices
     */
    private static HashSet<Vertex> getAllAccessors(Accessor accessor) {

        HashSet<Vertex> all = Sets.newHashSet();
        if (!isAnonymous(accessor)) {
            Iterable<Accessor> parents = accessor.getAllParents();
            for (Accessor a : parents) {
                all.add(a.asVertex());
            }
            all.add(accessor.asVertex());
        }
        return all;
    }

    /**
     * Fetch a user's global permission set.
     *
     * @param accessor The user/group
     * @return A global permission set for the given user.
     */
    private GlobalPermissionSet getAccessorPermissions(Accessor accessor) {
        GlobalPermissionSet.Builder builder = GlobalPermissionSet.newBuilder();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            PermissionScope scope = grant.getScope();
            if (scope == null || scopes.contains(scope)) {
                for (PermissionGrantTarget target : grant.getTargets()) {
                    if (manager.getEntityClass(target).equals(EntityClass.CONTENT_TYPE)) {
                        ContentType contentType = target.as(ContentType.class);
                        Permission permission = grant.getPermission();
                        if (permission != null) {
                            builder.set(
                                    contentTypeEnumMap.getUnchecked(contentType),
                                    permissionEnumMap.getUnchecked(permission));
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    /**
     * Get the data structure for admin permissions, which is basically all
     * available permissions turned on.
     *
     * @return A global permission set for the admin user.
     */
    private GlobalPermissionSet getAdminPermissions() {
        GlobalPermissionSet.Builder builder = GlobalPermissionSet.newBuilder();
        for (ContentTypes ct : ContentTypes.values()) {
            builder.set(ct, PermissionType.values());
        }
        return builder.build();
    }

    /**
     * Pipe filter function that passes through all items.
     *
     * @return A no-op PipeFunction for filtering a list of vertices as an admin
     * user
     */
    private static PipeFunction<Vertex, Boolean> noopFilterFunction() {
        return v -> true;
    }

    // Get a list of the current scope and its parents
    private HashSet<PermissionScope> getAllScopes() {
        HashSet<PermissionScope> all = Sets.newHashSet(scope.getPermissionScopes());
        if (!isSystemScope()) {
            all.add(scope);
        }
        return all;
    }

    private boolean isSystemScope() {
        return scope.equals(SystemScope.INSTANCE);
    }

    private boolean isInScope(PermissionGrant grant) {
        // If we're on the system scope, only remove unscoped
        // permissions. Otherwise, check the scope matches the grant.
        return isSystemScope()
                && grant.getScope() == null
                || (grant.getScope() != null && Iterables.contains(scopes,
                grant.getScope()));
    }

    /**
     * Get the content type with the given id.
     */
    private ContentTypes getContentType(EntityClass type) {
        try {
            return ContentTypes.withName(type.getName());
        } catch (NoSuchElementException e) {
            throw new RuntimeException(
                    String.format("No content type found for type: '%s'", type.getName()), e);
        }
    }

    private boolean hasContentTypeTargets(PermissionGrant grant) {
        for (PermissionGrantTarget tg : grant.getTargets()) {
            if (!manager.getEntityClass(tg).equals(EntityClass.CONTENT_TYPE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPromoted(Vertex v) {
        int promotions = Iterables.size(v.getEdges(Direction.OUT, Ontology.PROMOTED_BY));
        return promotions > 0
                && promotions > Iterables.size(v.getEdges(Direction.OUT, Ontology.DEMOTED_BY));
    }
}
