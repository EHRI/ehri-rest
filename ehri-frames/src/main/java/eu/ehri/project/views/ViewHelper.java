package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
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

    private final FramedGraph<?> graph;
    private final PermissionScope scope;
    private final AclManager acl;
    private final GraphManager manager;

    public ViewHelper(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    public ViewHelper(FramedGraph<?> graph, PermissionScope scope) {
        Preconditions.checkNotNull(scope);
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
        if (!acl.hasPermission(ctype, permType, accessor)) {
            throw new PermissionDenied(accessor.getId(), ctype.toString(), permType.toString(), scope.getId());
        }
    }

    /**
     * Check permissions for a given entity.
     *
     * @throws PermissionDenied
     */
    public void checkEntityPermission(AccessibleEntity entity,
            Accessor accessor, PermissionType permType) throws PermissionDenied {
        if (!acl.hasPermission(entity, permType, accessor)) {
            throw new PermissionDenied(accessor.getId(), entity.getId(),
                        permType.toString(), scope.getId());
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

    public ContentTypes getContentType(Class<?> cls) {
        return ContentTypes.withName(ClassUtils.getEntityType(cls).getName());
    }

    /**
     * @return the acl
     */
    public AclManager getAclManager() {
        return acl;
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
        return new ViewHelper(graph, Optional
                .fromNullable(scope).or(SystemScope.INSTANCE));
    }
}
