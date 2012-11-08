package eu.ehri.project.exceptions;

import eu.ehri.project.models.Permission;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

public class PermissionDenied extends Exception {

    private static final long serialVersionUID = -3948097018322416889L;

    private Accessor accessor = null;
    private AccessibleEntity entity = null;
    private PermissionScope scope = null;
    private Permission permission = null;

    public PermissionDenied(Accessor accessor, String message) {
        super(String.format("Permission denied accessing resource as '%s': %s",
                accessor.getIdentifier(), message));
        this.accessor = accessor;
    }

    public PermissionDenied(String message) {
        super(message);
    }

    public PermissionDenied(Accessor accessor, AccessibleEntity entity) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s'",
                entity.toString(), accessor.toString()));
        this.accessor = accessor;
        this.entity = entity;
    }

    public PermissionDenied(Accessor accessor, AccessibleEntity entity,
            Permission permission, PermissionScope scope) {
        super(
                String.format(
                        "Permission '%s' denied for resource '%s' as '%s' with scope '%s'",
                        permission.getIdentifier(), entity.getIdentifier(),
                        accessor.getIdentifier(), scope.getIdentifier()));
        this.accessor = accessor;
        this.entity = entity;
        this.scope = scope;
        this.permission = permission;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    public AccessibleEntity getEntity() {
        return entity;
    }

    public PermissionScope getScope() {
        return scope;
    }

    public Permission getPermission() {
        return permission;
    }
}
