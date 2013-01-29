package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

import static eu.ehri.project.persistance.ActionManager.LIFECYCLE_EVENT;

@EntityType(EntityClass.GLOBAL_EVENT)
public interface GlobalEvent extends AccessibleEntity {

    public static enum EventType {
        lifecycleEvent, interactionEvent
    }

    public final String TIMESTAMP = "timestamp";
    public final String LOG_MESSAGE = "logMessage";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    @Fetch(value = Action.HAS_EVENT_ACTION, ifDepth = 0)
    @Adjacency(label = Action.HAS_EVENT_ACTION)
    public Action getAction();
}
