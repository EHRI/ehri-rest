package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.EmptyIterable;

/**
 * Implementation of an anonymous user.
 * 
 * @author michaelb
 *
 */
public enum AnonymousAccessor implements Accessor {
    
    INSTANCE;

    private AnonymousAccessor() {}
    
    /**
     * Obtain the shared instance of the Anonymous Accessor.
     * @return
     */
    public static Accessor getInstance() {
        return INSTANCE;
    }
        
    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public String getIdentifier() {
        return Group.ANONYMOUS_GROUP_IDENTIFIER;
    }

    public Iterable<Accessor> getParents() {
        return new EmptyIterable<Accessor>();
    }

    public Iterable<Accessor> getAllParents() {
        return new EmptyIterable<Accessor>();
    }

    public Iterable<AccessibleEntity> getAccessibleEntities() {
        return new EmptyIterable<AccessibleEntity>();
    }

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<PermissionGrant>();
    }

    public void addPermissionGrant(PermissionGrant grant) {
        throw new UnsupportedOperationException();
    }
}
