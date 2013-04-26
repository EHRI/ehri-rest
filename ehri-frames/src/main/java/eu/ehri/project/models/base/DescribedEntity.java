package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

public interface DescribedEntity extends AccessibleEntity, IdentifiableEntity, AnnotatableEntity, LinkableEntity {

    public static final String DESCRIBES = "describes";

    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void addDescription(final Description description);

    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void setDescriptions(Iterable<Description> descriptions);

    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void removeDescription(final Description description);

    @Fetch(DESCRIBES)
    @Dependent
    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public Iterable<Description> getDescriptions();
}
