package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

public interface DescribedEntity extends VertexFrame, AnnotatableEntity {
    
    public static final String DESCRIBES = "describes";
    
    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void addDescription(final Description description);

    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void setDescriptions(Iterable<Description> descriptions);

    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public void removeDescription(final Description description);
}
