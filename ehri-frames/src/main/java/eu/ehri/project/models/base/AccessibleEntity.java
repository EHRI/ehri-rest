package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.utils.JavaHandlerUtils;

import static eu.ehri.project.models.utils.JavaHandlerUtils.addSingleRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.addUniqueRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.hasEdge;

public interface AccessibleEntity extends PermissionGrantTarget, VersionedEntity, AnnotatableEntity {

    @Fetch(value = Ontology.IS_ACCESSIBLE_TO, depth = 1)
    @Adjacency(label = Ontology.IS_ACCESSIBLE_TO)
    public Iterable<Accessor> getAccessors();

    /**
     * only Accessor accessor can access this AccessibleEntity.
     * This is NOT the way to add an Accessor to a Group, use Group.addMember()
     * @param accessor 
     */
    @JavaHandler
    public void addAccessor(final Accessor accessor);

    @Adjacency(label = Ontology.IS_ACCESSIBLE_TO)
    public void removeAccessor(final Accessor accessor);

    @Adjacency(label = Ontology.HAS_PERMISSION_SCOPE)
    public PermissionScope getPermissionScope();

    @JavaHandler
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

    @Fetch(value = Ontology.ENTITY_HAS_LIFECYCLE_EVENT, ifDepth = 0)
    @JavaHandler
    public SystemEvent getLatestEvent();

    @JavaHandler
    boolean hasAccessRestriction();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, AccessibleEntity {

        public void addAccessor(final Accessor accessor) {
            addUniqueRelationship(it(), accessor.asVertex(),
                    Ontology.IS_ACCESSIBLE_TO);
        }

        public void setPermissionScope(final PermissionScope scope) {
            addSingleRelationship(it(), scope.asVertex(),
                    Ontology.HAS_PERMISSION_SCOPE);
        }

        public SystemEvent getLatestEvent() {
            GremlinPipeline<Vertex, Vertex> out = gremlin()
                    .out(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .out(Ontology.ENTITY_HAS_EVENT);
            return (SystemEvent)(out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<PermissionScope> getPermissionScopes() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.HAS_PERMISSION_SCOPE)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }

        public Iterable<SystemEvent> getHistory() {
            return frameVertices(gremlin().as("n").out(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc)
                    .out(Ontology.ENTITY_HAS_EVENT));
        }

        public boolean hasAccessRestriction() {
            return hasEdge(it(), Direction.OUT, Ontology.IS_ACCESSIBLE_TO)
                    && !hasEdge(it(), Direction.OUT, Ontology.PROMOTED_BY);
        }
    }
}
