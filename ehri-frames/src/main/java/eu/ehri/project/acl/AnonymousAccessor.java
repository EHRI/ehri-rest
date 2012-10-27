package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionAssertion;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.EmptyIterable;
import eu.ehri.project.relationships.Access;

/**
 * Implementation of an anonymous user.
 * 
 * @author michaelb
 *
 */
public class AnonymousAccessor implements Accessor {
    
    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return Group.ANONYMOUS_GROUP_NAME;
    }

    public void setName() {
        throw new UnsupportedOperationException();
    }

    public Iterable<Accessor> getParents() {
        return new EmptyIterable<Accessor>();
    }

    public Iterable<Accessor> getAllParents() {
        return new EmptyIterable<Accessor>();
    }

    public Iterable<Access> getAccess() {
        return new EmptyIterable<Access>();
    }

    public Iterable<AccessibleEntity> getAccessibleEntities() {
        return new EmptyIterable<AccessibleEntity>();
    }

    public void removeAccessibleEntity(AccessibleEntity entity) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PermissionAssertion> getPermissionGrants() {
        return new EmptyIterable<PermissionAssertion>();
    }

    public void addPermissionGrant(PermissionAssertion grant) {
        throw new UnsupportedOperationException();
    }
}
