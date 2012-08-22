package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.relationships.Access;

public interface Accessor {
    public static final String BELONGS_TO = "belongsTo";

    @Property("name")
    public String getName();

    @Property("name")
    public void setName();

    @Adjacency(label = BELONGS_TO)
    public Iterable<Accessor> getParents();

    @Incidence(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public Iterable<Access> getAccess();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public Iterable<AccessibleEntity> getAccessibleEntities();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public void addAccessibleEntity(final AccessibleEntity entity);

}
