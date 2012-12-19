package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.Action;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Unique;

public interface AccessibleEntity extends VertexFrame, PermissionGrantTarget {

    public static final String ACCESS = "access";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String HAS_SCOPE = "hasScope";

    @Unique
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();

    @Fetch(depth=1)
    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessors();

    @Adjacency(label = ACCESS)
    public void addAccessor(final Accessor accessor);

    @Adjacency(label = ACCESS)
    public void removeAccessor(final Accessor accessor);

    @Adjacency(label = PermissionGrant.HAS_ENTITY)
    public Iterable<PermissionGrant> getPermissionAssertions();

    @Adjacency(label = Action.HAS_SUBJECT, direction = Direction.IN)
    public Iterable<Action> getHistory();
    
    @Adjacency(label = HAS_SCOPE)
    public PermissionScope getScope();
    
    @Adjacency(label = HAS_SCOPE)
    public void setScope(final PermissionScope scope);
    
    @GremlinGroovy("_().as('n').out('" + HAS_SCOPE
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<PermissionScope> getScopes();    
}
