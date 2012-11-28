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
public class AnonymousAccessor implements Accessor {

    private AnonymousAccessor() {}
    
    private static class AnonymousAccessorHolder {
        public static final Accessor INSTANCE = new AnonymousAccessor();
    }
    
    /**
     * Obtain the shared instance of the Anonymous Accessor.
     * @return
     */
    public static Accessor getInstance() {
        return AnonymousAccessorHolder.INSTANCE;
    }
        
    public Vertex asVertex() {
        throw new UnsupportedOperationException();
    }

    public String getIdentifier() {
        return Group.ANONYMOUS_GROUP_IDENTIFIER;
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

    public Iterable<AccessibleEntity> getAccessibleEntities() {
        return new EmptyIterable<AccessibleEntity>();
    }

    public void removeAccessibleEntity(AccessibleEntity entity) {
        throw new UnsupportedOperationException();
    }

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<PermissionGrant>();
    }

    public void addPermissionGrant(PermissionGrant grant) {
        throw new UnsupportedOperationException();
    }
}
