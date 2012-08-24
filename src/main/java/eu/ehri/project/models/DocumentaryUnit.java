package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.annotations.EntityType;

@EntityType(EntityTypes.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AccessibleEntity, DescribedEntity,
        TemporalEntity {

    public static final String CHILD_OF = "childOf";

    @Adjacency(label = Agent.HOLDS, direction = Direction.IN)
    public Agent getAgent();

    @Adjacency(label = Agent.HOLDS, direction = Direction.IN)
    public void setAgent(final Agent institution);

    @Adjacency(label = DocumentaryUnit.CHILD_OF)
    public DocumentaryUnit getParent();

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @GremlinGroovy("_().as('n').out('" + CHILD_OF + "').loop('n'){it.loops < 20}{true}")
    public Iterable<DocumentaryUnit> getAncestors();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getChildren();
    
    @Adjacency(label = Authority.CREATED, direction = Direction.IN)
    public Iterable<Authority> getCreators();
    
    @Adjacency(label = Authority.CREATED, direction = Direction.IN)
    public void addCreator(final Authority creator);
    
    @Adjacency(label = Authority.MENTIONED_IN, direction = Direction.IN)
    public Iterable<Authority> getNameAccess();    

    @Adjacency(label = Authority.MENTIONED_IN, direction = Direction.IN)
    public void addNameAccess(final Authority nameAccess);
}
