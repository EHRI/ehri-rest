package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.PermissionAssertion;

public interface PermissionScope extends VertexFrame {
    @Adjacency(label = PermissionAssertion.HAS_SCOPE, direction=Direction.IN)
    public Iterable<PermissionAssertion> getPermissions();
}
