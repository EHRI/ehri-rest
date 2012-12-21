package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.AGENT)
public interface Agent extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity, PermissionScope {

    public static final String HELDBY = "heldBy";

    @Adjacency(label = HELDBY, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getCollections();

    @Adjacency(label = HELDBY, direction = Direction.IN)
    public void addCollection(final TemporalEntity collection);
}
