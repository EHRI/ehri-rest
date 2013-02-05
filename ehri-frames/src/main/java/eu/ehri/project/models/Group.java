package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

@EntityType(EntityClass.GROUP)
public interface Group extends VertexFrame, Accessor, AccessibleEntity,
        PermissionScope {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";

    @Fetch(BELONGS_TO)
    @Adjacency(label = BELONGS_TO)
    public Iterable<Group> getGroups();
    
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public Iterable<Accessor> getMembers();

    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public void addMember(final Accessor accessor);
    
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public void removeMember(final Accessor accessor);
    
    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @GremlinGroovy("_().as('n').in('belongsTo').loop('n'){true}{it.object.__ISA__=='userProfile'}")
    public Iterable<UserProfile> getAllUserProfileMembers();

}
