package eu.ehri.project.exceptions;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

public class PermissionDenied extends Exception {

    private static final long serialVersionUID = -3948097018322416889L;

    public PermissionDenied(Accessor accessor, String message) {
        super(String.format("Permission denied accessing resource as '%s': %s",
                accessor.getName(), message));
    }

    public PermissionDenied(Accessor accessor, AccessibleEntity entity) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s')",
                entity.getName(), accessor.getName()));
    }
}
