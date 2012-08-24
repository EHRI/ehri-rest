package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.relationships.Access;

public interface Accessor {
    public static final String BELONGS_TO = "belongsTo";

    @Property("name")
    public String getName();

    @Property("name")
    public void setName();

    @Adjacency(label = BELONGS_TO)
    public Iterable<Accessor> getParents();
    
    @GremlinGroovy("_().as('n').out('" + BELONGS_TO + "').loop('n'){it.loops < 20}{true}")
    public Iterable<Accessor> getAllParents();

    @Incidence(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public Iterable<Access> getAccess();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public Iterable<AccessibleEntity> getAccessibleEntities();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public void addAccessibleEntity(final AccessibleEntity entity);

}
