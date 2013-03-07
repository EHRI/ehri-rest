package eu.ehri.project.exceptions;

import eu.ehri.project.models.Permission;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

public class PermissionDenied extends Exception {

    private static final long serialVersionUID = -3948097018322416889L;

    private String accessor = null;
    private String entity = null;
    private String scope = null;
    private String permission = null;

    public PermissionDenied(String accessor, String message) {
        super(String.format("Permission denied accessing resource as '%s': %s",
                accessor, message));
        this.accessor = accessor;
    }

    public PermissionDenied(String message) {
        super(message);
    }

    public PermissionDenied(String accessor, String entity, String message) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s': %s",
                entity, accessor, entity, message));
        this.accessor = accessor;
        this.entity = entity;
    }

    public PermissionDenied(String accessor, String entity,
            String permission, String scope) {
        super(
                String.format(
                        "Permission '%s' denied for resource '%s' as '%s' with scope '%s'",
                        permission, entity,
                        accessor, scope));
        this.accessor = accessor;
        this.entity = entity;
        this.scope = scope;
        this.permission = permission;
    }

    public String getAccessor() {
        return accessor;
    }

    public String getEntity() {
        return entity;
    }

    public String getScope() {
        return scope;
    }

    public String getPermission() {
        return permission;
    }
}
