package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

@EntityType(EntityTypes.GROUP)
public interface Group extends VertexFrame, Accessor, AccessibleEntity {

    public static final String isA = "group";

    public static final String ADMIN_GROUP_NAME = "admin";
    public static final String ANONYMOUS_GROUP_NAME = "anonymous";

    @Adjacency(label = UserProfile.BELONGS_TO, direction = Direction.IN)
    public Iterable<UserProfile> getUsers();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);
}
