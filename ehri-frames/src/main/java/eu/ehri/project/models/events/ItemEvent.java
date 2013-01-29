package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.ActionManager;

import static eu.ehri.project.persistance.ActionManager.*;

@EntityType(EntityClass.ACTION_EVENT)
public interface ItemEvent extends AccessibleEntity {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_HISTORY = "hasHistory";

    public static final String HAS_GLOBAL_EVENT = "hasGlobalEvent";

    @Fetch(Action.HAS_EVENT_ACTION)
    @Adjacency(label = Action.HAS_EVENT_ACTION)
    public Action getAction();

    @Fetch(value=HAS_GLOBAL_EVENT, ifDepth = 0)
    @Adjacency(label = HAS_GLOBAL_EVENT)
    public GlobalEvent getGlobalEvent();

    @Fetch(value = HAS_SUBJECT, ifDepth = 0)
    @GremlinGroovy("_().as('e').in('" + LIFECYCLE_EVENT
            + "').loop('e'){true}{it.object.in('"
            + LIFECYCLE_EVENT + "').count()==0}")
    public Iterable<AccessibleEntity> getSubject();
}
