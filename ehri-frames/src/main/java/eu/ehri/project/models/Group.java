package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;

@EntityType(EntityClass.GROUP)
public interface Group extends Accessor, AccessibleEntity,
        PermissionScope {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";

    @Fetch(BELONGS_TO)
    @Adjacency(label = BELONGS_TO)
    public Iterable<Group> getGroups();

    /**
     * TODO FIXME use this in case we need AccesibleEnity's instead of Accessors, 
     * but we should change the inheritance instead! 
     * 
     * Note: 
     * All directly related 'belongsTo' are return now, 
     * in order to traverse the tree and only get UserProfile's we could do something like: 
     * @GremlinGroovy("_().as('n').in('belongsTo').loop('n'){true}{it.object.__ISA__=='userProfile'}")
     * public Iterable<UserProfile> getAllUserProfileMembers();      
     */
    @Adjacency(label = BELONGS_TO, direction = Direction.IN)
    public Iterable<AccessibleEntity> getMembersAsEntities();

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

    // FIXME: Use of __ISA__ here breaks encapsulation of indexing details quite horribly
    @GremlinGroovy("_().as('n').in('belongsTo').loop('n'){true}{it.object.__ISA__=='userProfile'}")
    public Iterable<UserProfile> getAllUserProfileMembers();

}
