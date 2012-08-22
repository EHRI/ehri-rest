package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface DescribedEntity {
    @Adjacency(label = "describes", direction = Direction.IN)
    public Iterable<Description> getDescriptions();

    @Adjacency(label = "describes", direction = Direction.IN)
    public void addDescription(final Description description);

    @Adjacency(label = "describes", direction = Direction.IN)
    public void setDescriptions(Iterable<Description> descriptions);

}
