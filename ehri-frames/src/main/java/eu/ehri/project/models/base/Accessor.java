package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.PermissionGrant;

public interface Accessor extends IdentifiableEntity {
    public static final String BELONGS_TO = "belongsTo";

    @Adjacency(label = BELONGS_TO)
    public Iterable<Accessor> getParents();

    @GremlinGroovy("_().as('n').out('" + BELONGS_TO
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<Accessor> getAllParents();
    
    @Adjacency(label = PermissionGrant.HAS_SUBJECT, direction=Direction.IN)
    public Iterable<PermissionGrant> getPermissionGrants();

    @Adjacency(label = PermissionGrant.HAS_SUBJECT, direction=Direction.IN)
    public void addPermissionGrant(final PermissionGrant grant);
}
