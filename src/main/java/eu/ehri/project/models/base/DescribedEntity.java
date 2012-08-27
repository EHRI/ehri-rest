package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.CascadeDelete;
import eu.ehri.project.models.annotations.Fetch;

public interface DescribedEntity extends VertexFrame {
    @Fetch
    @CascadeDelete
    @Adjacency(label = "describes", direction = Direction.IN)
    public Iterable<Description> getDescriptions();

    @Adjacency(label = "describes", direction = Direction.IN)
    public void addDescription(final Description description);

    @Adjacency(label = "describes", direction = Direction.IN)
    public void setDescriptions(Iterable<Description> descriptions);

}
