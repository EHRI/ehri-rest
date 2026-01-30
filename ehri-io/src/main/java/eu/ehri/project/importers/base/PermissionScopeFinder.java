package eu.ehri.project.importers.base;

import eu.ehri.project.models.base.PermissionScope;

import java.util.function.Function;

public interface PermissionScopeFinder extends Function<String, PermissionScope> {
    /**
     * Retrieve the permission scope for the given identifier.
     *
     * @param localId the local identifier
     *
     * @return a permission scope instance
     */
    PermissionScope apply(String localId);
}
