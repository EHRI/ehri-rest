package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.relationships.Access;

public interface Accessor {
    @Property("name")
    public String getName();

    @Property("name")
    public void setName();

    @Adjacency(label = "belongsTo")
    public Iterable<Accessor> getParents();

    @Incidence(label = "access", direction = Direction.IN)
    public Iterable<Access> getAccess();

    @Adjacency(label = "access", direction = Direction.IN)
    public Iterable<Entity> getAccessibleEntities();

    @Adjacency(label = "access", direction = Direction.IN)
    public void addAccessibleEntity(final Entity entity);
}
