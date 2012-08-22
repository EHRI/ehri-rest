package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface DocumentaryUnit extends AccessibleEntity, DescribedEntity,
        TemporalEntity {

    public static final String isA = "documentaryUnit";
    public static final String CHILD_OF = "childOf";

    @Adjacency(label = Agent.HOLDS, direction = Direction.IN)
    public Agent getAgent();

    @Adjacency(label = Agent.HOLDS, direction = Direction.IN)
    public void setAgent(final Agent institution);

    @Adjacency(label = DocumentaryUnit.CHILD_OF)
    public DocumentaryUnit getParent();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getChildren();
}
