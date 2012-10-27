package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.PermissionAssertion;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.EmptyIterable;

public class SystemScope implements PermissionScope {

    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public Iterable<PermissionAssertion> getPermissions() {
        return new EmptyIterable<PermissionAssertion>();
    }

}
