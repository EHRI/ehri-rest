package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.EmptyIterable;

public class SystemScope implements PermissionScope {

    public static final String SYSTEM = "system";

    public String getIdentifier() {
        return SYSTEM;
    }

    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public Iterable<PermissionGrant> getPermissions() {
        return new EmptyIterable<PermissionGrant>();
    }

}
