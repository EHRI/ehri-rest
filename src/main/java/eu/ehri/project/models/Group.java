package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Group extends Accessor {

    public static final String isA = "group";
    
    @Adjacency(label = UserProfile.BELONGS_TO, direction = Direction.IN)
    public Iterable<UserProfile> getUsers();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);
}
