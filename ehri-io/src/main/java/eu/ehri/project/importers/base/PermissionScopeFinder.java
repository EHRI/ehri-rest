package eu.ehri.project.importers.base;

import eu.ehri.project.models.base.PermissionScope;

public interface PermissionScopeFinder {
    /**
     * Retrieve the permission scope for the given identifier.
     *
     * @param localId the local identifier
     *
     * @return a permission scope instance
     */
    PermissionScope get(String localId);
}
