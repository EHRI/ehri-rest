package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.PermissionGrant;

public interface PermissionScope {
    @Adjacency(label = PermissionGrant.HAS_SCOPE, direction = Direction.IN)
    public Iterable<PermissionGrant> getPermissions();

    @Property(AccessibleEntity.IDENTIFIER_KEY)
    public String getIdentifier();
}
