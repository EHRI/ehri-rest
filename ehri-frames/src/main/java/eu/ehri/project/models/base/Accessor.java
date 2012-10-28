package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.PermissionGrant;

public interface Accessor extends VertexFrame {
    public static final String BELONGS_TO = "belongsTo";
    
    public static final String NAME = "name";

    @Property(NAME)
    public String getName();

    @Property(NAME)
    public void setName();

    @Adjacency(label = BELONGS_TO)
    public Iterable<Accessor> getParents();

    @GremlinGroovy("_().as('n').out('" + BELONGS_TO
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<Accessor> getAllParents();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public Iterable<AccessibleEntity> getAccessibleEntities();

    @Adjacency(label = AccessibleEntity.ACCESS, direction = Direction.IN)
    public void removeAccessibleEntity(final AccessibleEntity entity);
    
    @Adjacency(label = PermissionGrant.HAS_ACCESSOR, direction=Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();

    @Adjacency(label = PermissionGrant.HAS_ACCESSOR, direction=Direction.IN)
    public void addPermissionGrant(final PermissionGrant grant);
}
