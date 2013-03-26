package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.PermissionGrant;

public interface PermissionGrantTarget extends Frame {
    @Adjacency(label=PermissionGrant.HAS_TARGET, direction=Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();
}
