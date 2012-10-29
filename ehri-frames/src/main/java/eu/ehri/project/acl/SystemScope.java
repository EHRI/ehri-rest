package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.Action;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.EmptyIterable;

public class SystemScope implements PermissionScope, AccessibleEntity {

    public static final String SYSTEM = "system";

    public String getIdentifier() {
        return SYSTEM;
    }

    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<PermissionGrant>();
    }

    public Iterable<Accessor> getAccessors() {
        return new EmptyIterable<Accessor>();
    }

    public void addAccessor(Accessor accessor) {
        throw new UnsupportedOperationException();
        
    }

    public void removeAccessor(Accessor accessor) {
        throw new UnsupportedOperationException();
        
    }

    public Iterable<PermissionGrant> getPermissionAssertions() {
        return new EmptyIterable<PermissionGrant>();
    }

    public Iterable<Action> getHistory() {
        return new EmptyIterable<Action>();
    }

}
