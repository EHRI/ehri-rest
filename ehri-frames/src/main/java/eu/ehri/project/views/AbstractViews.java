package eu.ehri.project.views;

import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.relationships.Access;

abstract class AbstractViews<E extends AccessibleEntity> {

    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;

    /**
     * @param graph
     * @param cls
     */
    public AbstractViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }

    /**
     * Check permissions for a given type.
     * 
     * @throws PermissionDenied
     */
    protected void checkPermission(Long user, Long scope, String permissionId)
            throws PermissionDenied {
        Accessor accessor = getAccessor(user);
        PermissionScope permScope = getPermissionScope(scope);
        // If we're admin, the answer is always "no problem"!
        if (!acl.isAdmin(accessor)) {
            String etype = ClassUtils.getEntityType(cls);
            ContentType ctype;
            try {
                ctype = graph
                        .getVertices(AccessibleEntity.IDENTIFIER_KEY, etype,
                                ContentType.class).iterator().next();
            } catch (NoSuchElementException e) {
                throw new RuntimeException(String.format(
                        "No content type node found for type: '%s'", etype), e);
            }
            Permission permission;
            try {
                permission = graph
                        .getVertices(AccessibleEntity.IDENTIFIER_KEY,
                                permissionId, Permission.class).iterator()
                        .next();
            } catch (NoSuchElementException e) {
                throw new RuntimeException(String.format(
                        "No permission found for name: '%s'", permissionId), e);
            }
            Iterable<PermissionGrant> perms = acl.getPermissions(accessor,
                    ctype, permission);
            
            boolean found = false;
            for (PermissionGrant perm : perms) {
                // If the permission has unscoped rights, the user is
                // good to do whatever they want to do here.
                Iterable<PermissionScope> scopes = perm.getScopes();
                if (!scopes.iterator().hasNext()) {
                    found = true;
                    break;
                }

                // Otherwise, verify that the given scope is included.
                for (PermissionScope s : scopes) {
                    if (s.equals(permScope)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new PermissionDenied(String.format(
                        "Permission '%s' denied with scope: '%s'", permission,
                        permScope));
            }
        }
    }

    private PermissionScope getPermissionScope(Long id) {
        if (id == null)
            return new SystemScope();
        // FIXME: Ensure this item really is a permission scope!
        return graph.frame(graph.getVertex(id), PermissionScope.class);
    }

    /**
     * Ensure an item is readable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkReadAccess(AccessibleEntity entity, Long user)
            throws PermissionDenied {
        Accessor accessor = getAccessor(user);
        Access access = acl.getAccessControl(entity, accessor);
        if (!access.getRead())
            throw new PermissionDenied(accessor, entity);
    }

    /**
     * Ensure an item is writable by the given user
     * 
     * @param entity
     * @param user
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, Long user)
            throws PermissionDenied {
        Accessor accessor = getAccessor(user);
        Access access = acl.getAccessControl(entity, accessor);
        if (!(access.getRead() && access.getWrite()))
            throw new PermissionDenied(accessor, entity);
    }

    protected void checkGlobalWriteAccess(long user) throws PermissionDenied {
        // TODO: Stub
    }

    protected Accessor getAccessor(Long id) {
        if (id == null)
            return new AnonymousAccessor();
        // FIXME: Ensure this item really is an accessor!
        return graph.frame(graph.getVertex(id), Accessor.class);
    }
}
