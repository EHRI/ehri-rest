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
import eu.ehri.project.persistance.ActionManager;

@EntityType(EntityClass.ACTION)
public interface Action extends AccessibleEntity {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_ACTIONER = "hasActioner";
    public static String HAS_GLOBAL_EVENT = "hasGlobalEvent";
    public static final String HAS_EVENT_ACTION = "hasEventAction";


    @Fetch(value=HAS_GLOBAL_EVENT, ifDepth = 0)
    @Adjacency(label = HAS_GLOBAL_EVENT)
    public GlobalEvent getGlobalEvent();

    /**
     * Fetch the subjects associated with an action. This means going from the
     * Action node to any associated Event nodes, and traversing up their event
     * chain to each object.
     * 
     * @return
     */
    @GremlinGroovy("_().in('" + HAS_EVENT_ACTION + "')"
            + ".as('e').in('" + ActionManager.LIFECYCLE_EVENT
            + "').loop('e'){true}{true}")
    public Iterable<AccessibleEntity> getSubjects();

    @Adjacency(label = HAS_EVENT_ACTION, direction = Direction.IN)
    public Iterable<ItemEvent> getActionEvents();

    @Fetch(value = HAS_ACTIONER, ifDepth = 0)
    @GremlinGroovy("_().as('e').in('" + ActionManager.LIFECYCLE_ACTION
            + "').loop('e'){true}{it.object.in('"
            + ActionManager.LIFECYCLE_ACTION + "').count()==0}")
    public Iterable<Actioner> getActioner();
}
