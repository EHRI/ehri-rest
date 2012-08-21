package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;

import eu.ehri.project.relationships.Access;

public interface Group extends Accessor {

    @Adjacency(label = "belongsTo", direction = Direction.IN)
    public Iterable<UserProfile> getUsers();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Adjacency(label = "access")
    public void removeEntity(final Entity entity);

    @Adjacency(label = "access")
    public void setEntities(Iterable<Entity> entities);

    @Adjacency(label = "access")
    public void addEntity(final Entity entity);

    @Incidence(label = "access")
    public Iterable<Access> getAccess();

}
