package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.utils.JavaHandlerUtils;
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

    @JavaHandler
    public Iterable<PermissionScope> getPermissionScopes();

    /**
     * Fetch a list of Actions for this entity in order.
     * 
     * @return
     */
    @JavaHandler
    public Iterable<SystemEvent> getHistory();

    @Fetch(value = ActionManager.LIFECYCLE_EVENT, ifDepth = 0)
    @JavaHandler
    public SystemEvent getLatestEvent();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, AccessibleEntity {
        public SystemEvent getLatestEvent() {
            GremlinPipeline<Vertex, Vertex> out = gremlin()
                    .out(ActionManager.LIFECYCLE_EVENT)
                    .out(SystemEvent.HAS_EVENT);
            return (SystemEvent)(out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<PermissionScope> getPermissionScopes() {
            return frameVertices(gremlin().as("n")
                    .out(HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }

        public Iterable<SystemEvent> getHistory() {
            return frameVertices(gremlin().as("n").out(ActionManager.LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc)
                    .out(SystemEvent.HAS_EVENT));
        }
    }
}
