package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerImpl;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import eu.ehri.project.persistance.ActionManager;

@EntityType(EntityClass.SYSTEM_EVENT)
public interface SystemEvent extends AccessibleEntity {

    public static final String HAS_EVENT = "hasEvent";
    public static final String HAS_ACTIONER = "hasActioner";
    public static final String HAS_EVENT_SCOPE = "hasEventScope";

    public static enum EventType {
        lifecycleEvent, interactionEvent
    }

    public final String TIMESTAMP = "timestamp";
    public final String LOG_MESSAGE = "logMessage";
    public final String EVENT_TYPE = "eventType";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(EVENT_TYPE)
    public String getEventType();

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    @Fetch(HAS_ACTIONER)
    @JavaHandler
    public Iterable<Actioner> getActioners();

    @JavaHandler
    public Iterable<AccessibleEntity> getSubjects();

    /**
     * Fetch the "scope" of this event, or the context in which a
     * given creation/modification/deletion event is happening.
     * @return
     */
    @Fetch(value = HAS_EVENT_SCOPE, ifDepth = 0)
    @Adjacency(label = HAS_EVENT_SCOPE, direction = Direction.OUT)
    public Frame getEventScope();

    @Adjacency(label = HAS_EVENT_SCOPE, direction = Direction.OUT)
    public void setEventScope(final Frame frame);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerImpl<Vertex>, SystemEvent {
        public Iterable<AccessibleEntity> getSubjects() {
            return frameVertices(gremlin().in(HAS_EVENT)
                    .as("n").in(ActionManager.LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.IN,
                                    ActionManager.LIFECYCLE_EVENT).iterator().hasNext();
                        }
                    }));
        }

        public Iterable<Actioner> getActioners() {
            return frameVertices(gremlin().in(HAS_EVENT)
                    .as("n").in(ActionManager.LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.IN,
                                    ActionManager.LIFECYCLE_ACTION).iterator().hasNext();
                        }
                    }));
        }
    }
}
