package eu.ehri.project.acl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Helper class for checking and asserting access and write permissions.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class AclManager {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final HashSet<Vertex> scopes;

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
    public AclManager(FramedGraph<?> graph, PermissionScope scope) {
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
    public AclManager(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     * @return User belongs to the admin group
     */
    public boolean belongsToAdmin(Accessor accessor) {
        if (accessor.isAdmin())
            return true;
        for (Accessor parent : accessor.getParents()) {
            if (belongsToAdmin(parent))
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
                || (!isAnonymous(accessor) && accessor.equals(entity))) {
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
            if (acc.equals(accessor))
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
    public List<Map<String, GlobalPermissionSet>> getInheritedGlobalPermissions(
            Accessor accessor) {
        List<Map<String, GlobalPermissionSet>> globals = Lists.newLinkedList();
        Map<String, GlobalPermissionSet> userMap = Maps.newHashMap();
        userMap.put(accessor.getId(), getGlobalPermissions(accessor));
        globals.add(userMap);
        for (Accessor parent : accessor.getParents()) {
            Map<String, GlobalPermissionSet> parentMap = Maps
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
    public GlobalPermissionSet getGlobalPermissions(Accessor accessor) {
        return belongsToAdmin(accessor)
                ? getAdminPermissions()
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
            manager.deleteVertex(maybeGrant.get().asVertex());
        }
    }
    
    /**
     * Revoke a particular permission grant.
     * 
     * @param grant
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
        // TODO: This lookup could be cached
        final Set<String> typeStrings = Sets.newHashSet();
        for (ContentTypes t : ContentTypes.values()) {
            typeStrings.add(t.getName());
        }
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex v) {
                Preconditions.checkNotNull(v);
                if (v == null)
                    return false;
                return typeStrings.contains(manager.getType(v));
            }
        };
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
                        Ontology.IS_ACCESSIBLE_TO);
                // If there's no Access conditions, it's
                // read-only...
                if (!verts.iterator().hasNext())
                    return true;
                // If it's promoted it's publically accessible
                if (v.getEdges(Direction.OUT, Ontology.PROMOTED_BY).iterator().hasNext())
                    return true;

                // Otherwise, check relevant accessors...
                for (Vertex other : verts) {
                    if (all.contains(other))
                        return true;
                }
                return false;
            }
        };
    }

    /**
     * Check if a user has permission to perform an action on the given content type.
     * @param contentType       The content type
     * @param permissionType    The requested permission
     * @param accessor          The user
     * @return
     */
    public boolean hasPermission(ContentTypes contentType, PermissionType permissionType, Accessor accessor) {
        return hasPermission(contentType, permissionType, accessor, scopes);
    }

    /**
     * Check if a user has permission to perform an action on the given item.
     * @param entity            The item
     * @param permissionType    The requested permission
     * @param accessor          The user
     * @return
     */
    public boolean hasPermission(AccessibleEntity entity, PermissionType permissionType, Accessor accessor) {

        // Get a list of our current context scopes, plus
        // the parent scopes of the item.
        Set<Vertex> allScopes = Sets.newHashSet(scopes);
        for (PermissionScope scope : entity.getPermissionScopes()) {
            allScopes.add(scope.asVertex());
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
     * Check for a content permission with a given set of scopes.
     * @param contentType
     * @param permissionType
     * @param accessor
     * @param scopes
     * @return
     */
    private boolean hasPermission(ContentTypes contentType, PermissionType permissionType, Accessor accessor,
            Collection<Vertex> scopes) {

        ContentType contentTypeNode = enumContentTypeMap.get(contentType);
        // Check the user themselves...
        return belongsToAdmin(accessor) || hasScopedPermission(contentTypeNode, permissionType, accessor, scopes);
    }

    /**
     * Attempt to find a permission, searching the accessor's parent hierarchy.
     * @param target
     * @param permissionType
     * @param accessor
     * @param scopes
     * @return
     */
    private boolean hasScopedPermission(PermissionGrantTarget target, PermissionType permissionType, Accessor accessor,
            Collection<Vertex> scopes) {

        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            PermissionType grantPermissionType = enumForPermission(grant.getPermission());

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
                if (grant.getScope() == null || scopes.contains(grant.getScope().asVertex())) {
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
        if (belongsToAdmin(accessor)) {
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

    private PermissionGrant createPermissionGrant() throws IntegrityError {
        Vertex vertex = manager.createVertex(
                EntityClass.PERMISSION_GRANT.getIdgen()
                        .generateId(SystemScope.getInstance(), null),
                EntityClass.PERMISSION_GRANT,
                Maps.<String, Object> newHashMap());
        return graph.frame(vertex, PermissionGrant.class);
    }

    private void checkNoGrantOnAdminOrAnon(Accessor accessor)
            throws PermissionDenied {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        if (accessor.isAdmin() || accessor.isAnonymous())
            throw new PermissionDenied(
                    "Unable to grant or revoke permissions to system accounts.");
    }

    private void assertNoGrantOnAdminOrAnon(Accessor accessor) {
        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        // This time throw  a runtime error.
        if (accessor.isAdmin() || accessor.isAnonymous())
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
    private GlobalPermissionSet getAccessorPermissions(
            Accessor accessor) {
        Multimap<ContentTypes, PermissionType> permmap = HashMultimap.create();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            // Since these are global perms only include those where the target
            // is a content type. FIXME: if it has been deleted, the target
            // could well be null.
            PermissionScope scope = grant.getScope();
            if (scope == null || scopes.contains(scope.asVertex())) {
                for (PermissionGrantTarget target : grant.getTargets()) {
                    if (manager.getEntityClass(target).equals(EntityClass.CONTENT_TYPE)) {
                        Permission permission = grant.getPermission();
                        if (permission != null) {
                            permmap.put(
                                    contentTypeEnumMap.get(target.asVertex()),
                                    permissionEnumMap.get(permission.asVertex()));
                        }
                    }
                }
            }
        }
        return new GlobalPermissionSet(permmap);
    }

    /**
     * Get the data structure for admin permissions, which is basically all
     * available permissions turned on.
     * 
     * @return
     */
    private GlobalPermissionSet getAdminPermissions() {
        Multimap<ContentTypes, PermissionType> perms = HashMultimap.create();
        for (ContentTypes ct : ContentTypes.values()) {
            perms.putAll(ct, Arrays.asList(PermissionType.values()));
        }
        return new GlobalPermissionSet(perms);
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
    private HashSet<Vertex> getAllScopes() {
        HashSet<Vertex> all = Sets.newHashSet();
        for (PermissionScope s : scope.getPermissionScopes()) {
            all.add(s.asVertex());
        }
        // Maybe remove systemscope completely...???
        if (!scope.equals(SystemScope.getInstance())) {
            all.add(scope.asVertex());
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
                        grant.getScope().asVertex()));
    }

    /**
     * Get the content type with the given id.
     *
     * @param type
     * @return
     */
    private ContentTypes getContentType(EntityClass type) {
        try {
            return ContentTypes.withName(type.getName());
        } catch (NoSuchElementException e) {
            throw new RuntimeException(
                    String.format("No content type found for type: '%s'", type.getName()), e);
        }
    }
}
