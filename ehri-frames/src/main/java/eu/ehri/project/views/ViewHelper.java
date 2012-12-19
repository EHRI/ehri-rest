package eu.ehri.project.views;

import java.util.Collection;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;

public final class ViewHelper {

    private final FramedGraph<Neo4jGraph> graph;
    private final Class<?> cls;
    private final PermissionScope scope;
    private final Collection<Vertex> scopes;
    private final AclManager acl;
    private final GraphManager manager;

    public ViewHelper(FramedGraph<Neo4jGraph> graph, Class<?> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    public ViewHelper(FramedGraph<Neo4jGraph> graph, Class<?> cls,
            PermissionScope scope) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
        this.scope = scope;
        this.scopes = getAllScopes(scope);
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Check permissions for a given type.
     * 
     * @throws PermissionDenied
     */
    public void checkPermission(Accessor accessor, PermissionType permType)
            throws PermissionDenied {
        // If we're admin, the answer is always "no problem"!
        if (!acl.belongsToAdmin(accessor)) {
            Permission permission = getPermission(permType);
            ContentType contentType = getContentType(ClassUtils
                    .getEntityType(cls));
            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    contentType, permission);
            boolean found = false;
            for (PermissionGrant perm : perms) {
                // If the permission has unscoped rights, or the
                // current scope contains the permission scope, the
                // user can proceed.
                PermissionScope permScope = perm.getScope();
                if (permScope == null || scopes.contains(permScope.asVertex())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new PermissionDenied(accessor, contentType, permission,
                        scope);
            }
        }
    }

    /**
     * Check permissions for a given entity.
     * 
     * @throws PermissionDenied
     */
    public void checkEntityPermission(AccessibleEntity entity,
            Accessor accessor, PermissionType permType) throws PermissionDenied {

        // TODO: Determine behaviour for granular item-level
        // attributes.
        try {
            checkPermission(accessor, permType);
        } catch (PermissionDenied e) {
            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    entity, getPermission(permType));
            // Scopes do not apply to entity-level perms...
            if (!perms.iterator().hasNext())
                throw new PermissionDenied(accessor, entity);
        }

    }

    /**
     * Ensure an item is readable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    public void checkReadAccess(AccessibleEntity entity, Accessor user)
            throws PermissionDenied {
        if (!acl.getAccessControl(entity, user))
            throw new PermissionDenied(user, entity);
    }

    /**
     * Ensure an item is writable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, Accessor accessor)
            throws PermissionDenied {
        checkEntityPermission(entity, accessor, PermissionType.UPDATE);
    }

    /**
     * Get the content type with the given id.
     * 
     * @param typeName
     * @return
     */
    public ContentType getContentType(EntityClass type) {
        try {
            return manager.getFrame(type.getName(), ContentType.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(
                    String.format("No content type node found for type: '%s'",
                            type.getName()), e);
        }
    }

    /**
     * Fetch any item of a particular type by its identifier.
     * 
     * @param typeName
     * @param name
     * @param cls
     * @return
     * @throws ItemNotFound
     */
    public <T> T getEntity(EntityClass type, String name, Class<T> cls)
            throws ItemNotFound {
        return manager.getFrame(name, type, cls);
    }

    /**
     * Get the permission with the given string.
     * 
     * @param permissionId
     * @return
     */
    public Permission getPermission(PermissionType perm) {
        try {
            return manager.getFrame(perm.getName(), EntityClass.PERMISSION,
                    Permission.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(String.format(
                    "No permission found for name: '%s'", perm.getName()), e);
        }
    }

    /**
     * Set the scope under which ACL and permission operations will take place.
     * This is, for example, an Agent instance, where the objects being
     * manipulated are DocumentaryUnits. The given scope is used to compare
     * against the scope relation on PermissionGrants.
     * 
     * @param scope
     */
    public ViewHelper setScope(PermissionScope scope) {
        return new ViewHelper(graph, cls, scope);
    }

    // Get a list of the current scope and its parents
    public static Collection<Vertex> getAllScopes(PermissionScope scope) {
        Collection<Vertex> all = Lists.newArrayList();
        for (PermissionScope s : scope.getScopes()) all.add(s.asVertex());
        all.add(scope.asVertex());
        return all;
    }
}
