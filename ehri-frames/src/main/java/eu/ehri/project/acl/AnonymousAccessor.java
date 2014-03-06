package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.EmptyIterable;

/**
 * Implementation of an anonymous user singleton.
 * 
 * @author michaelb
 *
 */
public enum AnonymousAccessor implements Accessor {
    
    INSTANCE;

    private AnonymousAccessor() {}

    public boolean isAdmin() {
        return false;
    }

    public boolean isAnonymous() {
        return true;
    }

    /**
     * Obtain the shared instance of the Anonymous Accessor.
     * There Can Be Only One.
     */
    public static Accessor getInstance() {
        return INSTANCE;
    }

    public String getId() {
        return Group.ANONYMOUS_GROUP_IDENTIFIER;
    }

    public String getType() {
        return Entities.GROUP;
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

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<PermissionGrant>();
    }

    public void addPermissionGrant(PermissionGrant grant) {
        throw new UnsupportedOperationException();
    }
}
