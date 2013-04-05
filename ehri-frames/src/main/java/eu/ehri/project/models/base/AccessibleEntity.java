package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;

public interface AccessibleEntity extends PermissionGrantTarget {

    public static final String ACCESS = "access";
    public static final String HAS_PERMISSION_SCOPE = "hasPermissionScope";

    @Fetch(value = ACCESS, depth = 1)
    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessors();

    /**
     * only Accessor accessor can access this AccessibleEntity.
     * This is NOT the way to add an Accessor to a Group, use Group.addMember()
     * @param accessor 
     */
    @Adjacency(label = ACCESS)
    public void addAccessor(final Accessor accessor);

    @Adjacency(label = ACCESS)
    public void removeAccessor(final Accessor accessor);

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
            + ".loop('n'){true}{true}.out('" + eu.ehri.project.models.events.SystemEvent.HAS_EVENT + "')")
    public Iterable<SystemEvent> getHistory();

    // FIXME: This should be a single item return but frames doesn't currently
    // support those...
    @Fetch(value = ActionManager.LIFECYCLE_EVENT, ifDepth = 1)
    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_EVENT + "')"
            + ".out('" + eu.ehri.project.models.events.SystemEvent.HAS_EVENT + "')")
    public Iterable<SystemEvent> getLatestEvent();
}
