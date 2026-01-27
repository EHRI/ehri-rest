package eu.ehri.project.importers;

import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.models.base.PermissionScope;

/**
 * A permission scope finder that always returns a specific scope.
 */
public class StaticPermissionScopeFinder implements PermissionScopeFinder {
    private final PermissionScope scope;

    public StaticPermissionScopeFinder(PermissionScope scope) {
        this.scope = scope;
    }

    @Override
    public PermissionScope get(String localId) {
        return scope;
    }
}
