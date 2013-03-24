package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Messy stopgap class to hold a bunch of sort-of view/sort-of acl functions.
 * <p/>
 * TODO: Clarify, consolidate and remove this class.
 *
 * @author mike
 */
public final class ViewHelper {

    private final FramedGraph<Neo4jGraph> graph;
    private final PermissionScope scope;
    private final AclManager acl;
    private final GraphManager manager;

    public ViewHelper(FramedGraph<Neo4jGraph> graph) {
        this(graph, SystemScope.getInstance());
    }

    public ViewHelper(FramedGraph<Neo4jGraph> graph, PermissionScope scope) {
        this.graph = graph;
        this.acl = new AclManager(graph, scope);
        this.scope = scope;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Check permissions for a given type.
     *
     * @param accessor
     * @param ctype
     * @param permType
     * @throws PermissionDenied
     */
    public void checkContentPermission(Accessor accessor, ContentTypes ctype,
            PermissionType permType) throws PermissionDenied {
        // If we're admin, the answer is always "no problem"!
        if (!acl.belongsToAdmin(accessor)) {
            Permission permission = getPermission(permType);

            ContentType contentType = getContentType(ctype);

            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    contentType, permission);
            if (Iterables.isEmpty(perms)) {
                throw new PermissionDenied(accessor.getId(), contentType.getId(),
                        permission.getId(), scope.getId());
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
            checkContentPermission(accessor, getContentType(entity), permType);
        } catch (PermissionDenied e) {
            Permission permission = getPermission(permType);
            Iterable<PermissionGrant> perms = acl.getPermissionGrants(accessor,
                    entity, permission);
            // Scopes do not apply to entity-level perms...
            if (Iterables.isEmpty(perms)) {
                throw new PermissionDenied(accessor.getId(), entity.getId(),
                        permission.getId(), scope.getId());
            }
        }

    }

    /**
     * Ensure an item is readable by the given user
     *
     * @param entity
     * @param user
     * @throws AccessDenied
     */
    public void checkReadAccess(AccessibleEntity entity, Accessor user)
            throws AccessDenied {
        if (!acl.getAccessControl(entity, user)) {
            // Using 'fake' permission 'read'
            throw new AccessDenied(user.getId(), entity.getId());
        }
    }

    /**
     * Ensure an item is writable by the given user
     *
     * @param entity
     * @param accessor
     * @throws PermissionDenied
     */
    protected void checkWriteAccess(AccessibleEntity entity, Accessor accessor)
            throws PermissionDenied {
        checkEntityPermission(entity, accessor, PermissionType.UPDATE);
    }

    /**
     * Get the content type with the given id.
     *
     * @param type
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
     * Deduce content type from the given enum.
     *
     * @param type
     * @return
     */
    public ContentType getContentType(ContentTypes type) {
        return getContentType(type.getName());
    }

    /**
     * Get the content type node for the given enum.
     *
     * @param typeName
     * @return
     */
    public ContentType getContentType(String typeName) {
        try {
            return manager.getFrame(typeName, ContentType.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(String.format(
                    "No content type node found for type: '%s'", typeName), e);
        }
    }

    public ContentTypes getContentType(Class<?> cls) {
        return ContentTypes.withName(ClassUtils.getEntityType(cls).getName());
    }

    public ContentTypes getContentType(Frame frame) {
        EntityClass et = manager.getType(frame);
        try {
            return ContentTypes.withName(et.getName());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format(
                    "No content type found for node of class: '%s'", et), e);
        }
    }

    /**
     * @return the acl
     */
    public AclManager getAclManager() {
        return acl;
    }

    /**
     * Get the permission with the given string.
     *
     * @param permission
     * @return
     */
    public Permission getPermission(PermissionType permission) {
        try {
            return manager.getFrame(permission.getName(), EntityClass.PERMISSION,
                    Permission.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(String.format(
                    "No permission found for name: '%s'", permission.getName()), e);
        }
    }

    /**
     * Set the scope under which ACL and permission operations will take place.
     * This is, for example, an Repository instance, where the objects being
     * manipulated are DocumentaryUnits. The given scope is used to compare
     * against the scope relation on PermissionGrants.
     *
     * @param scope
     */
    public ViewHelper setScope(PermissionScope scope) {
        return new ViewHelper(graph, scope);
    }
}
