package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistance.ActionManager;

@EntityType(EntityClass.ACTION)
public interface Action extends AccessibleEntity {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_ACTION_EVENT = "hasActionEvent";
    public static final String HAS_ACTIONER = "hasActioner";
    public static final String HAS_PRIOR_ACTION = "hasPriorAction";

    public final String TIMESTAMP = "timestamp";
    public final String LOG_MESSAGE = "logMessage";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    /**
     * Fetch the subjects associated with an action. This means going from the
     * Action node to any associated Event nodes, and traversing up their event
     * chain to each object.
     * 
     * @return
     */
    // @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_ACTION +
    // "')" +
    // ".loop('n'){true}{true}.in('" + ActionManager.HAS_EVENT_ACTION + "')" +
    // ".as('e').in('" + ActionManager.LIFECYCLE_EVENT +
    // "').loop('e'){true}{true}")
    @GremlinGroovy("_().in('" + ActionManager.HAS_EVENT_ACTION + "')"
            + ".as('e').in('" + ActionManager.LIFECYCLE_EVENT
            + "').loop('e'){true}{true}")
    public Iterable<AccessibleEntity> getSubjects();

    @Adjacency(label = HAS_ACTION_EVENT)
    public Iterable<ActionEvent> getActionEvent();

    @Fetch(ifDepth = 0)
    @GremlinGroovy("_().as('e').in('" + ActionManager.LIFECYCLE_ACTION
            + "').loop('e'){true}{true}.sideEffect{x=it}.filter{x.__ISA__=='" + Entities.USER_PROFILE + "'}")
    public Iterable<Actioner> getActioner();
}
