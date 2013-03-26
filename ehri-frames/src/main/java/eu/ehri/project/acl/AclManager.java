package eu.ehri.project.acl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Helper class for checking and asserting access and write permissions.
 * 
 * TODO: Re-express all this hideousness as Cypher or Gremlin queries, though
 * they will inevitably be quite complex.
 * 
 * @author mike
 * 
 */
public final class AclManager {

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final Collection<Vertex> scopes;

    // Lookups to convert between the enum and node representations
    // of content and permission types.
    private final Map<PermissionType, Permission> enumPermissionMap = Maps
            .newEnumMap(PermissionType.class);
    private final Map<ContentTypes, ContentType> enumContentTypeMap = Maps
            .newEnumMap(ContentTypes.class);
    private final Map<Vertex, PermissionType> permissionEnumMap = Maps
            .newHashMap();
    private final Map<Vertex, ContentTypes> contentTypeEnumMap = Maps
            .newHashMap();

    /**
     * Scoped constructor.
     * 
     * @param graph
     */
    public AclManager(FramedGraph<Neo4jGraph> graph, PermissionScope scope) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.scope = Optional.<PermissionScope> fromNullable(scope).or(
                SystemScope.getInstance());
        this.scopes = getAllScopes();
        populateEnumNodeLookups();
    }

    /**
     * Constructor.
     * 
     * @param graph
     */
    public AclManager(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Check if the current accessor IS the admin group.
     * 
     * @param accessor
     */
    public boolean isAdmin(Accessor accessor) {
        Preconditions.checkNotNull(accessor, "NULL accessor given.");
        return accessor.getId().equals(Group.ADMIN_GROUP_IDENTIFIER);
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     * @return User belongs to the admin group
     */
    public boolean belongsToAdmin(Accessor accessor) {
        if (isAdmin(accessor))
            return true;
        for (Accessor parent : accessor.getAllParents()) {
            if (isAdmin(parent))
                return true;
        }
        return false;
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     * @return User is an anonymous accessor
     */
    public boolean isAnonymous(Accessor accessor) {
        Preconditions.checkNotNull(accessor, "NULL accessor given.");
        return accessor instanceof AnonymousAccessor
                || accessor.getId().equals(
                        Group.ANONYMOUS_GROUP_IDENTIFIER);
    }

    /**
     * Find the access permissions for a given accessor and entity.
     * 
     * @param entity
     * @param accessor
     * 
     * @return Whether or not the given accessor can access the entity
     */
    public boolean getAccessControl(AccessibleEntity entity, Accessor accessor) {
        Preconditions.checkNotNull(entity, "Entity is null");
        Preconditions.checkNotNull(accessor, "Accessor is null");
        // Admin can read/write everything and object can always read/write
        // itself
        // FIXME: Tidy up the logic here.
        if (belongsToAdmin(accessor)
                || (!isAnonymous(accessor) && accessor.asVertex().equals(
                        entity.asVertex()))) {
            return true;
        }

        // Otherwise, check if there are specified permissions.
        List<Accessor> accessors = new ArrayList<Accessor>();
        for (Accessor acc : entity.getAccessors()) {
            accessors.add(acc);
        }

        if (accessors.isEmpty()) {
            return true;
        } else if (isAnonymous(accessor)) {
            return false;
        } else {
            List<Accessor> initial = new ArrayList<Accessor>();
            initial.add(accessor);
            return !searchAccess(initial, accessors).isEmpty();
        }
    }

    /**
     * Revoke an accessor's access to an entity.
     * 
     * @param entity
     * @param accessor
     */
    public void removeAccessControl(AccessibleEntity entity, Accessor accessor) {
        for (Accessor acc : entity.getAccessors()) {
            if (acc.asVertex().equals(accessor.asVertex()))
                entity.removeAccessor(accessor);
        }
    }

    /**
     * Set access control on an entity to several accessors.
     * 
     * @param entity
     * @param accessors
     */
    public void setAccessors(AccessibleEntity entity,
            Iterable<Accessor> accessors) {
        // FIXME: Must be a more efficient way to do this, whilst
        // ensuring that superfluous double relationships don't get created?
        Set<Vertex> accessorVertices = Sets.newHashSet();
        for (Accessor acc : accessors)
            accessorVertices.add(acc.asVertex());

        Set<Vertex> existing = Sets.newHashSet();
        Set<Vertex> remove = Sets.newHashSet();
        for (Accessor accessor : entity.getAccessors()) {
            Vertex v = accessor.asVertex();
            existing.add(v);
            if (!accessorVertices.contains(v)) {
                remove.add(v);
            }
        }
        for (Vertex v : remove) {
            entity.removeAccessor(graph.frame(v, Accessor.class));
        }
        for (Accessor accessor : accessors) {
            if (!existing.contains(accessor.asVertex())) {
                entity.addAccessor(accessor);
            }
        }
    }

    /**
     * Get a list of PermissionGrants for the given user on the given target.
     * This list includes PermissionGrants inherited from parent groups.
     * 
     * @param accessor
     * @param entity
     * @param permission
     * 
     * @return Iterable of PermissionGrant instances for the given accessor,
     *         target, and permission
     */
    public Iterable<PermissionGrant> getPermissionGrants(Accessor accessor,
            AccessibleEntity entity, Permission permission) {

        // FIXME: Although passed in as a PermissionGrantTarget, the proxy item
        // may not originally have been framed as that, but instead as a
        // subclass.
        // This means we have to cast it as a PermissionGrantTarget anyway.
        PermissionGrantTarget target = graph.frame(entity.asVertex(),
                PermissionGrantTarget.class);

        List<PermissionGrant> grants = Lists.newLinkedList();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {

            if (isGlobalOrInScope(grant)
                    && Iterables.contains(grant.getTargets(), target)) {
                PermissionType gt = enumForPermission(grant.getPermission());
                PermissionType pt = enumForPermission(permission);
                if (((gt.getMask() & pt.getMask()) == pt.getMask())) {
                    grants.add(grant);
                }
            }
        }

        for (Accessor parent : accessor.getParents()) {
            for (PermissionGrant grant : getPermissionGrants(parent, entity,
                    permission)) {
                grants.add(grant);
            }
        }
        return ImmutableSet.copyOf(grants);
    }

    /**
     * Get a list of permissions for a given accessor on a given entity. Returns
     * a map of content types against the grant permissions.
     * 
     * @param accessor
     * @return List of permission maps for the given target
     */
    public List<Map<String, List<PermissionType>>> getInheritedEntityPermissions(
            Accessor accessor, AccessibleEntity entity) {
        List<Map<String, List<PermissionType>>> list = Lists.newLinkedList();
        Map<String, List<PermissionType>> userMap = Maps.newHashMap();
        userMap.put(accessor.getId(), getEntityPermissions(accessor, entity));
        list.add(userMap);
        for (Accessor parent : accessor.getAllParents()) {
            Map<String, List<PermissionType>> parentMap = Maps.newHashMap();
            list.add(parentMap);
            parentMap.put(parent.getId(), getEntityPermissions(parent, entity));
        }
        return list;
    }

    /**
     * Set the permissions for a particular user on the given item.
     * 
     * @param accessor
     * @param item
     * @param permissionList
     * @throws PermissionDenied
     */
    public void setEntityPermissions(Accessor accessor, AccessibleEntity item,
            Set<PermissionType> permissionList) throws PermissionDenied {
        checkNoGrantOnAdminOrAnon(accessor);
        for (PermissionType t : PermissionType.values()) {
            if (permissionList.contains(t))
                grantPermissions(accessor, item, t);
            else
                revokePermissions(accessor, item, t);
        }
    }

    /**
     * Return a permission list for the given accessor and her inherited groups.
     * 
     * @param accessor
     * @return List of permission maps for the given accessor and his group
     *         parents.
     */
    public List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> getInheritedGlobalPermissions(
            Accessor accessor) {
        List<Map<String, Map<ContentTypes, Collection<PermissionType>>>> globals = Lists
                .newLinkedList();
        Map<String, Map<ContentTypes, Collection<PermissionType>>> userMap = Maps
                .newHashMap();
        userMap.put(accessor.getId(), getGlobalPermissions(accessor));
        globals.add(userMap);
        for (Accessor parent : accessor.getParents()) {
            Map<String, Map<ContentTypes, Collection<PermissionType>>> parentMap = Maps
                    .newHashMap();
            parentMap.put(parent.getId(), getGlobalPermissions(parent));
            globals.add(parentMap);
        }
        return globals;
    }

    /**
     * Recursive helper function to ascend an accessor's groups and populate
     * their global permissions.
     * 
     * @param accessor
     * @return Permission map for the given accessor
     */
    public Map<ContentTypes, Collection<PermissionType>> getGlobalPermissions(
            Accessor accessor) {
        return isAdmin(accessor) ? getAdminPermissions()
                : getAccessorPermissions(accessor);
    }

    /**
     * Set a matrix of global permissions for a given accessor.
     * 
     * @param accessor
     * @param globals
     *            global permission map
     * @throws PermissionDenied
     */
    public void setPermissionMatrix(Accessor accessor,
            Map<ContentTypes, List<PermissionType>> globals)
            throws PermissionDenied {
        checkNoGrantOnAdminOrAnon(accessor);

        for (Entry<ContentTypes, ContentType> centry : enumContentTypeMap
                .entrySet()) {
            ContentType target = centry.getValue();
            List<PermissionType> pset = globals.get(centry.getKey());
            if (pset == null)
                continue;
            for (PermissionType perm : PermissionType.values()) {
                if (pset.contains(perm)) {
                    grantPermissions(accessor, target, perm);
                } else {
                    revokePermissions(accessor, target, perm);
                }
            }
        }
    }

    /**
     * Grant a user permissions to a content type.
     * 
     * @param accessor
     * @param permType
     * @return The permission grant given for this accessor and target
     */
    public PermissionGrant grantPermissions(Accessor accessor,
            PermissionGrantTarget target, PermissionType permType) {
        assertNoGrantOnAdminOrAnon(accessor);
        // If we can find an existing grant, use that, otherwise create a new
        // one.
        Optional<PermissionGrant> maybeGrant = findPermission(accessor, target,
                permType);
        if (maybeGrant.isPresent()) {
            return maybeGrant.get();
        } else {
            try {
                PermissionGrant grant = createPermissionGrant();
                accessor.addPermissionGrant(grant);
                grant.setPermission(vertexForPermission(permType));
                grant.addTarget(target);
                if (!scope.equals(SystemScope.getInstance())) {
                    grant.setScope(scope);
                }
                return grant;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Revoke a particular permission grant.
     * 
     * @param accessor
     * @param permType
     */
    public void revokePermissions(Accessor accessor, AccessibleEntity entity,
            PermissionType permType) {

        Optional<PermissionGrant> maybeGrant = findPermission(accessor, entity,
                permType);
        if (maybeGrant.isPresent()) {
            graph.removeVertex(maybeGrant.get().asVertex());
        }
    }
    
    /**
     * Revoke a particular permission grant.
     * 
     * @param grant
     */
    public void revokePermissionGrant(PermissionGrant grant) {
        graph.removeVertex(grant.asVertex());
    }

    /**
     * Build a gremlin filter function that passes through items readable by a
     * given accessor.
     * 
     * @param accessor
     * @return A PipeFunction for filtering a set of vertices as the given user
     */
    public PipeFunction<Vertex, Boolean> getAclFilterFunction(Accessor accessor) {
        Preconditions.checkNotNull(accessor, "Accessor is null");
        if (belongsToAdmin(accessor))
            return noopFilterFunction();

        final HashSet<Vertex> all = getAllAccessors(accessor);
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex v) {
                Iterable<Vertex> verts = v.getVertices(Direction.OUT,
                        AccessibleEntity.ACCESS);
                // If there's no Access conditions, it's
                // read-only...
                if (!verts.iterator().hasNext())
                    return true;
                for (Vertex other : verts) {
                    if (all.contains(other))
                        return true;
                }
                return false;
            }
        };
    }

    // Helpers...

    /**
     * Set scope.
     * 
     * @param scope
     * @return
     */
    public AclManager withScope(PermissionScope scope) {
        return new AclManager(graph, scope);
    }

    /**
     * Get scope.
     * 
     * @return
     */
    public PermissionScope getScope() {
        return scope;
    }

    /**
     * Get the permission type enum for a given node.
     * 
     * @param perm
     * @return
     */
    private Permission vertexForPermission(PermissionType perm) {
        return enumPermissionMap.get(perm);
    }

    /**
     * Get the permission type enum for a given node.
     * 
     * @param perm
     * @return
     */
    private PermissionType enumForPermission(Frame perm) {
        return permissionEnumMap.get(perm.asVertex());
    }

    /**
     * Get a list of global permissions for a given accessor. Returns a map of
     * content types against the grant permissions.
     * 
     * @param accessor
     * @return List of permission names for the given accessor on the given
     *         target
     */
    private List<PermissionType> getEntityPermissions(Accessor accessor,
            AccessibleEntity entity) {
        // If we're admin, add it regardless.
        if (isAdmin(accessor)) {
            return Lists.newArrayList(PermissionType.values());
        } else {
            List<PermissionType> list = Lists.newLinkedList();
            // Cache a set of permission scopes. This is the hierarchy on which
            // permissions are granted. For most items it will contain zero
            // entries and thus be pretty fast, but for deeply nested
            // documentary units there might be quite a few.
            HashSet<Vertex> scopes = Sets.newHashSet();
            for (PermissionScope scope : entity.getPermissionScopes())
                scopes.add(scope.asVertex());

            PermissionGrantTarget target = graph.frame(entity.asVertex(),
                    PermissionGrantTarget.class);

            for (PermissionGrant grant : accessor.getPermissionGrants()) {
                if (Iterables.contains(grant.getTargets(), target)) {
                    list.add(enumForPermission(grant.getPermission()));
                } else if (grant.getScope() != null) {
                    // If there isn't a direct grant to the entity, search its
                    // parent scopes for an appropriate scoped permission
                    if (scopes.contains(grant.getScope().asVertex())) {
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
     * 
     * @param accessor
     * @param entity
     * @param permType
     * @return
     */
    private Optional<PermissionGrant> findPermission(Accessor accessor,
            PermissionGrantTarget entity, PermissionType permType) {
        PermissionGrantTarget target = graph.frame(entity.asVertex(),
                PermissionGrantTarget.class);

        Permission perm = enumPermissionMap.get(permType);
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (isInScope(grant)
                    && Iterables.contains(grant.getTargets(), target)
                    && grant.getPermission().equals(perm)) {
                return Optional.of(grant);
            }
        }
        return Optional.absent();
    }

    private PermissionGrant createPermissionGrant() throws IntegrityError,
            IdGenerationError {
        Vertex vertex = manager.createVertex(
                EntityClass.PERMISSION_GRANT.getIdgen().generateId(
                        EntityClass.PERMISSION_GRANT,
                        SystemScope.getInstance(), null),
                EntityClass.PERMISSION_GRANT,
                Maps.<String, Object> newHashMap());
        return graph.frame(vertex, PermissionGrant.class);
    }

    private void checkNoGrantOnAdminOrAnon(Accessor accessor)
            throws PermissionDenied {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        if (isAdmin(accessor) || isAnonymous(accessor))
            throw new PermissionDenied(
                    "Unable to grant or revoke permissions to system accounts.");
    }

    private void assertNoGrantOnAdminOrAnon(Accessor accessor) {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        // This time throw  a runtime error.
        if (isAdmin(accessor) || isAnonymous(accessor))
            throw new RuntimeException(
                    "Unable to grant or revoke permissions to system accounts.");
    }

    /**
     * Search the group hierarchy of the given accessors to find an intersection
     * with those who can access a resource.
     * 
     * @param accessing
     *            The user(s) accessing the resource
     * @return The accessors in the given list able to access the resource
     */
    private List<Accessor> searchAccess(List<Accessor> accessing,
            List<Accessor> allowedAccessors) {
        if (accessing.isEmpty()) {
            return new ArrayList<Accessor>();
        } else {
            List<Accessor> intersection = new ArrayList<Accessor>();
            for (Accessor acc : allowedAccessors) {
                if (accessing.contains(acc)) {
                    intersection.add(acc);
                }
            }

            List<Accessor> parentPerms = new ArrayList<Accessor>();
            parentPerms.addAll(intersection);
            for (Accessor acc : accessing) {
                List<Accessor> parents = new ArrayList<Accessor>();
                for (Accessor parent : acc.getAllParents())
                    parents.add(parent);
                parentPerms.addAll(searchAccess(parents, allowedAccessors));
            }

            return parentPerms;
        }
    }

    /**
     * For a given user, fetch a lookup of all the inherited accessors it
     * belongs to.
     * 
     * @param accessor
     * @return
     */
    private HashSet<Vertex> getAllAccessors(Accessor accessor) {

        final HashSet<Vertex> all = Sets.newHashSet();
        if (!isAnonymous(accessor)) {
            Iterable<Accessor> parents = accessor.getAllParents();
            for (Accessor a : parents)
                all.add(a.asVertex());
            all.add(accessor.asVertex());
        }
        return all;
    }

    /**
     * @param accessor
     * @return
     */
    private Map<ContentTypes, Collection<PermissionType>> getAccessorPermissions(
            Accessor accessor) {
        Multimap<ContentTypes, PermissionType> permmap = LinkedListMultimap
                .create();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            // Since these are global perms only include those where the target
            // is a content type. FIXME: if it has been deleted, the target
            // could well be null.
            if (isGlobalOrInScope(grant)) {
                for (PermissionGrantTarget target : grant.getTargets()) {
                    if (ClassUtils.hasType(target, EntityClass.CONTENT_TYPE)) {
                        ContentTypes ctype = ContentTypes.withName(manager
                                .getId(target));
                        permmap.put(ctype, PermissionType.withName(manager
                                .getId(grant.getPermission())));
                    }
                }
            }
        }
        return permmap.asMap();
    }

    /**
     * Get the data structure for admin permissions, which is basically all
     * available permissions turned on.
     * 
     * @return
     */
    private Map<ContentTypes, Collection<PermissionType>> getAdminPermissions() {
        Multimap<ContentTypes, PermissionType> perms = LinkedListMultimap
                .create();
        for (ContentTypes ct : ContentTypes.values()) {
            perms.putAll(ct, Collections.unmodifiableList(Arrays
                    .asList(PermissionType.values())));
        }
        return perms.asMap();
    }

    /**
     * Pipe filter function that passes through all items. TODO: Check if we
     * actually need this???
     * 
     * @return A no-op PipeFunction for filtering a list of vertices as an admin
     *         user
     */
    private PipeFunction<Vertex, Boolean> noopFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex v) {
                return true;
            }
        };
    }

    private void populateEnumNodeLookups() {
        // Build a lookup of content types and permissions keyed by their
        // identifier.
        for (ContentType c : manager.getFrames(EntityClass.CONTENT_TYPE,
                ContentType.class)) {
            ContentTypes ct = ContentTypes.withName(c.getId());
            enumContentTypeMap.put(ct, c);
            contentTypeEnumMap.put(c.asVertex(), ct);
        }
        for (Permission p : manager.getFrames(EntityClass.PERMISSION,
                Permission.class)) {
            PermissionType pt = PermissionType.withName(p.getId());
            enumPermissionMap.put(pt, p);
            permissionEnumMap.put(p.asVertex(), pt);
        }
    }

    // Get a list of the current scope and its parents
    private Collection<Vertex> getAllScopes() {
        Collection<Vertex> all = Lists.newArrayList();
        for (PermissionScope s : scope.getPermissionScopes())
            all.add(s.asVertex());
        all.add(scope.asVertex());
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
                        grant.getScope().asVertex()));
    }

    private boolean isGlobalOrInScope(PermissionGrant grant) {
        // If we're on the system scope, only remove unscoped
        // permissions. Otherwise, check the scope matches the grant.
        if (grant.getScope() == null)
            return true;
        return !isSystemScope()
                && Iterables.contains(scopes, grant.getScope().asVertex());
    }
}
