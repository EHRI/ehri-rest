package eu.ehri.project.exceptions;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

public class PermissionDenied extends Exception {

    private static final long serialVersionUID = -3948097018322416889L;
    
    private Accessor accessor = null;
    private AccessibleEntity entity = null;

    public PermissionDenied(Accessor accessor, String message) {
        super(String.format("Permission denied accessing resource as '%s': %s",
                accessor.getName(), message));
        this.accessor = accessor;
    }

    public PermissionDenied(String message) {
        super(message);
    }

    public PermissionDenied(Accessor accessor, AccessibleEntity entity) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s')",
                entity.toString(), accessor.toString()));
        this.accessor = accessor;
        this.entity = entity;
    }

    public Accessor getAccessor() {
        return accessor;
    }
    
    public AccessibleEntity getEntity() {
        return entity;
    }
}
