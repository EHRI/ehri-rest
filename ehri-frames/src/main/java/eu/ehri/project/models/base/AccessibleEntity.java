package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.events.GlobalEvent;
import eu.ehri.project.models.events.ItemEvent;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Unique;
import eu.ehri.project.persistance.ActionManager;

public interface AccessibleEntity extends VertexFrame, PermissionGrantTarget {

    public static final String ACCESS = "access";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String HAS_PERMISSION_SCOPE = "hasPermissionScope";

    @Unique
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();

    @Fetch(value = ACCESS, depth = 1)
    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessors();

    @Adjacency(label = ACCESS)
    public void addAccessor(final Accessor accessor);

    @Adjacency(label = ACCESS)
    public void removeAccessor(final Accessor accessor);

    @Adjacency(label = PermissionGrant.HAS_ENTITY)
    public Iterable<PermissionGrant> getPermissionAssertions();

    @Adjacency(label = HAS_PERMISSION_SCOPE)
    public PermissionScope getPermissionScope();

    @Adjacency(label = HAS_PERMISSION_SCOPE)
    public void setPermissionScope(final PermissionScope scope);

    @GremlinGroovy("_().as('n').out('" + HAS_PERMISSION_SCOPE
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<PermissionScope> getPermissionScopes();

    /**
     * Fetch a list of Actions for this entity in order.
     * 
     * @return
     */
    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_EVENT + "')"
            + ".loop('n'){true}{true}.out('" + GlobalEvent.HAS_EVENT + "')")
    public Iterable<GlobalEvent> getHistory();

    // FIXME: This should be a single item return but frames doesn't currently
    // support those...
    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_EVENT + "')"
            + ".out('" + GlobalEvent.HAS_EVENT + "')")
    public Iterable<GlobalEvent> getLatestEvent();
}
