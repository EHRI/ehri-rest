package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Frame;
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
    public final String ACTION_TYPE = "eventType";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(ACTION_TYPE)
    public String getActionType();

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    @Fetch(HAS_ACTIONER)
    @GremlinGroovy("_().in('" + HAS_EVENT + "')"
            + ".as('n').in('" + ActionManager.LIFECYCLE_ACTION
            + "').loop('n'){true}{!it.object.in('" + ActionManager.LIFECYCLE_ACTION + "').hasNext()}")
    public Iterable<Actioner> getActioners();

    @GremlinGroovy("_().in('" + HAS_EVENT + "')"
            + ".as('n').in('" + ActionManager.LIFECYCLE_EVENT
            + "').loop('n'){true}{!it.object.in('" + ActionManager.LIFECYCLE_EVENT + "').hasNext()}")
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
}
