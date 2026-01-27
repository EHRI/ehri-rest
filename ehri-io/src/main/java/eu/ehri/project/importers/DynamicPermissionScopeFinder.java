package eu.ehri.project.importers;

import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.lang.NotImplementedException;

import java.net.URI;

public class DynamicPermissionScopeFinder implements PermissionScopeFinder {
    private PermissionScope parent;
    private URI hierarchyFile;

    public DynamicPermissionScopeFinder(PermissionScope parent, URI hierarchyFile) {
        this.parent = parent;
        this.hierarchyFile = hierarchyFile;
    }

    @Override
    public PermissionScope get(String localId) {
        // FIXME:
        throw new NotImplementedException();
    }
}
