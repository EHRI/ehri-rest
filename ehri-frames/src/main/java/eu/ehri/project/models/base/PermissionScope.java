package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.PermissionGrant;

public interface PermissionScope extends IdentifiableEntity, AccessibleEntity {
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SCOPE, direction = Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();
}
