package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends VertexFrame, AccessibleEntity,
        DescribedEntity, TemporalEntity, PermissionScope {

    public static final String CHILD_OF = "childOf";
    public static final String NAME = "name";

    @Property(NAME)
    public String getName();

    @Property(NAME)
    public void setName(String name);

    // NB: This is only an iterable until we upgrade to frames 2.3.0
    // which supports GremlinGroovy annotations for single items
    @GremlinGroovy("_().copySplit(_(), _().as('n').out('" + CHILD_OF +"')"
            + ".loop('n'){true}{!it.object.out('" + CHILD_OF +"').hasNext()}"
            + ").exhaustMerge().out('" + Agent.HELDBY + "')")
    public Iterable<Agent> getAgent();

    @Adjacency(label = Agent.HELDBY)
    public void setAgent(final Agent institution);

    @Fetch(CHILD_OF)
    @Adjacency(label = CHILD_OF)
    public DocumentaryUnit getParent();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public void addChild(final DocumentaryUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @GremlinGroovy("_().as('n').out('" + CHILD_OF
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<DocumentaryUnit> getAncestors();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getChildren();

    @Fetch(Authority.CREATED)
    @Adjacency(label = Authority.CREATED, direction = Direction.IN)
    public Iterable<Authority> getCreators();

    @Adjacency(label = Authority.CREATED, direction = Direction.IN)
    public void addCreator(final Authority creator);

    @Fetch(Authority.MENTIONED_IN)
    @Adjacency(label = Authority.MENTIONED_IN, direction = Direction.IN)
    public Iterable<Authority> getNameAccess();

    @Adjacency(label = Authority.MENTIONED_IN, direction = Direction.IN)
    public void addNameAccess(final Authority nameAccess);
}
