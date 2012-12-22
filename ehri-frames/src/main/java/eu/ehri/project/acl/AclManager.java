package eu.ehri.project.acl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
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
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.AnnotationUtils;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Helper class for checking and asserting access and write permissions.
 * 
 * @author mike
 * 
 */
public class AclManager {

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;
    private final Map<PermissionType, Permission> enumPermissionMap = Maps
            .newEnumMap(PermissionType.class);
    private final Map<ContentTypes, ContentType> enumContentTypeMap = Maps
            .newEnumMap(ContentTypes.class);

    public AclManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Check if the current accessor IS the admin group.
     * 
     * @param accessor
     */
    public Boolean isAdmin(Accessor accessor) {
        if (accessor == null)
            throw new RuntimeException("NULL accessor given.");
        return accessor.getIdentifier().equals(Group.ADMIN_GROUP_IDENTIFIER);
    }

    /**
     * Check if an accessor is admin or a member of Admin.
     * 
     * @param accessor
     * @return User belongs to the admin group
     */
    public Boolean belongsToAdmin(Accessor accessor) {
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
    public Boolean isAnonymous(Accessor accessor) {
        if (accessor == null)
            throw new RuntimeException("NULL accessor given.");
        return accessor instanceof AnonymousAccessor
                || accessor.getIdentifier()
                        .equals(Group.ADMIN_GROUP_IDENTIFIER);
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
     * Find the access permissions for a given accessor and entity.
     * 
     * @param entity
     * @param accessor
     * 
     * @return Whether or not the given accessor can access the entity
     */
    public boolean getAccessControl(AccessibleEntity entity, Accessor accessor) {
        // Admin can read/write everything and object can always read/write
        // itself
        assert entity != null : "Entity is null";
        assert accessor != null : "Accessor is null";
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
            PermissionGrantTarget target, Permission permission) {
        List<PermissionGrant> grants = Lists.newLinkedList();
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (target.asVertex().equals(grant.getTarget().asVertex())) {

                // Fetch the permission types corresponding to the nodes.
                // FIXME: The identifier might not be the most stable way of
                // id'ing a permission type.
                PermissionType gt = PermissionType.withName(grant
                        .getPermission().getIdentifier());
                PermissionType pt = PermissionType.withName(permission
                        .getIdentifier());

                if (((gt.getMask() & pt.getMask()) == pt.getMask())) {
                    grants.add(grant);
                }
            }
        }

        for (Accessor parent : accessor.getParents()) {
            for (PermissionGrant grant : getPermissionGrants(parent, target,
                    permission)) {
                grants.add(grant);
            }
        }
        return ClassUtils.makeUnique(grants);
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
        userMap.put(manager.getId(accessor), getGlobalPermissions(accessor));
        globals.add(userMap);
        for (Accessor parent : accessor.getParents()) {
            Map<String, Map<ContentTypes, Collection<PermissionType>>> parentMap = Maps
                    .newHashMap();
            parentMap.put(manager.getId(parent), getGlobalPermissions(parent));
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
     *            Global permission map
     * @throws PermissionDenied
     */
    public void setGlobalPermissionMatrix(Accessor accessor,
            Map<ContentTypes, List<PermissionType>> globals)
            throws PermissionDenied {
        // Build a lookup of content types and permissions keyed by their
        // identifier.
        Map<ContentTypes, ContentType> cmap = Maps.newHashMap();
        for (ContentType c : manager.getFrames(EntityClass.CONTENT_TYPE,
                ContentType.class)) {
            cmap.put(ContentTypes.withName(manager.getId(c)), c);
        }
        Map<PermissionType, Permission> pmap = Maps.newHashMap();
        for (Permission p : manager.getFrames(EntityClass.PERMISSION,
                Permission.class)) {
            pmap.put(PermissionType.withName(manager.getId(p)), p);
        }

        // Quick sanity check to make sure we're not trying to add/remove
        // permissions from the admin or the anonymous accounts.
        if (isAdmin(accessor) || isAnonymous(accessor))
            throw new PermissionDenied(
                    "Unable to grant or revoke permissions to system accounts.");

        for (Entry<ContentTypes, ContentType> centry : cmap.entrySet()) {
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
        userMap.put(manager.getId(accessor),
                getEntityPermissions(accessor, entity));
        list.add(userMap);
        for (Accessor parent : accessor.getAllParents()) {
            Map<String, List<PermissionType>> parentMap = Maps.newHashMap();
            list.add(parentMap);
            parentMap.put(manager.getId(parent),
                    getEntityPermissions(parent, entity));
        }
        return list;
    }

    /**
     * Get a list of global permissions for a given accessor. Returns a map of
     * content types against the grant permissions.
     * 
     * @param accessor
     * @return List of permission names for the given accessor on the given
     *         target
     */
    public List<PermissionType> getEntityPermissions(Accessor accessor,
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
            for (PermissionScope scope : entity.getScopes())
                scopes.add(scope.asVertex());

            for (PermissionGrant grant : accessor.getPermissionGrants()) {
                if (entity.asVertex().equals(grant.getTarget().asVertex())) {
                    list.add(PermissionType.withName(manager.getId(grant
                            .getPermission())));
                } else if (grant.getScope() != null) {
                    // If there isn't a direct grant to the entity, search its
                    // parent scopes for an appropriate scoped permission
                    if (scopes.contains(grant.getScope().asVertex())) {
                        list.add(PermissionType.withName(manager.getId(grant
                                .getPermission())));
                    }
                }
            }
            return list;
        }
    }

    /**
     * Grant a user permissions to a content type.
     * 
     * @param accessor
     * @param contentType
     * @param permType
     * @return The permission grant given for this accessor and target
     */
    public PermissionGrant grantPermissions(Accessor accessor,
            PermissionGrantTarget target, PermissionType permType) {
        PermissionGrant grant = graph.addVertex(null, PermissionGrant.class);
        accessor.addPermissionGrant(grant);
        grant.setPermission(getPermission(permType));
        grant.setTarget(target);
        return grant;
    }

    /**
     * Grant a user permissions to a content type, with the given scope.
     * 
     * @param accessor
     * @param contentType
     * @param permission
     * @param scope
     * @return The permission grant given for this accessor, target, and scope
     */
    public PermissionGrant grantPermissions(Accessor accessor,
            PermissionGrantTarget target, PermissionType permission,
            PermissionScope scope) {
        Preconditions.checkNotNull(scope, "Scope given to grantPermissions is null");
        PermissionGrant grant = grantPermissions(accessor, target, permission);
        if (!scope.getIdentifier().equals(SystemScope.SYSTEM))
            grant.setScope(scope);
        return grant;
    }

    /**
     * Revoke a particular permission grant.
     * 
     * @param accessor
     * @param target
     * @param permType
     */
    public void revokePermissions(Accessor accessor,
            PermissionGrantTarget target, PermissionType permType) {
        Permission perm = getPermission(permType);
        for (PermissionGrant grant : accessor.getPermissionGrants()) {
            if (grant.getTarget().asVertex().equals(target.asVertex())
                    && grant.getPermission().equals(perm)) {
                graph.removeVertex(grant.asVertex());
            }
        }
    }

    /**
     * Set access control on an entity.
     * 
     * @param entity
     * @param accessor
     * @param canRead
     * @param canWrite
     * @throws PermissionDenied
     */
    public void setAccessControl(AccessibleEntity entity, Accessor accessor)
            throws PermissionDenied {
        entity.addAccessor(accessor);
    }

    /**
     * Revoke an accessors access to an entity.
     * 
     * @param entity
     * @param accessor
     */
    public void removeAccessControl(AccessibleEntity entity, Accessor accessor) {
        entity.removeAccessor(accessor);
    }

    /**
     * Set access control on an entity to several accessors.
     * 
     * @param entity
     * @param accessors
     * @param canRead
     * @param canWrite
     * @throws PermissionDenied
     */
    public void setAccessControl(AccessibleEntity entity, Accessor[] accessors)
            throws PermissionDenied {
        for (Accessor accessor : accessors)
            entity.addAccessor(accessor);
    }

    /**
     * Build a gremlin filter function that passes through items readable by a
     * given accessor.
     * 
     * @param accessor
     * @return A PipeFunction for filtering a set of vertices as the given user
     */
    public PipeFunction<Vertex, Boolean> getAclFilterFunction(Accessor accessor) {
        assert accessor != null;
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

    /**
     * For a given user, fetch a lookup of all the inherited accessors it
     * belongs to.
     * 
     * @param user
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
            // is a content type
            PermissionGrantTarget target = grant.getTarget();
            if (grant.getScope() == null
                    && AnnotationUtils.hasFramedInterface(target,
                            ContentType.class)) {
                ContentTypes ctype = ContentTypes.withName(manager
                        .getId(target));
                permmap.put(ctype, PermissionType.withName(manager.getId(grant
                        .getPermission())));
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
     * Get the permission node for a given type.
     * 
     * @param permissionId
     * @return
     */
    public Permission getPermission(PermissionType perm) {
        try {
            Permission pt = enumPermissionMap.get(perm);
            if (pt == null) {
                pt = manager.getFrame(perm.getName(), EntityClass.PERMISSION,
                        Permission.class);
                enumPermissionMap.put(perm, pt);
            }
            return pt;
        } catch (ItemNotFound e) {
            throw new RuntimeException(String.format(
                    "No permission found for name: '%s'", perm.getName()), e);
        }
    }

    /**
     * Get the content type node for a given enum.
     * 
     * @param permissionId
     * @return
     */
    public ContentType getContentType(ContentTypes contentType) {
        try {
            ContentType ct = enumContentTypeMap.get(contentType);
            if (ct == null) {
                ct = manager.getFrame(contentType.getName(),
                        EntityClass.CONTENT_TYPE, ContentType.class);
                enumContentTypeMap.put(contentType, ct);
            }
            return ct;
        } catch (ItemNotFound e) {
            throw new RuntimeException(String.format(
                    "No content type found for name: '%s'",
                    contentType.getName()), e);
        }
    }

    /**
     * Get the permission type enum for a given node.
     * 
     * @param permissionId
     * @return
     */
    public PermissionType getPermissionType(Permission perm) {
        return PermissionType.withName(manager.getId(perm));
    }

    /**
     * Get the content type enum for a given node.
     * 
     * @param permissionId
     * @return
     */
    public ContentTypes getContentTypes(ContentType contentType) {
        return ContentTypes.withName(manager.getId(contentType));
    }

}
