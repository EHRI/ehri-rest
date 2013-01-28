package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.ActionManager;

@EntityType(EntityClass.ACTION_EVENT)
public interface ActionEvent extends AccessibleEntity {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_HISTORY = "hasHistory";
    public static final String HAS_PRIOR_EVENT = "hasPriorEvents";

    // FIXME: These properties are duplicated in Action
    // It's definitely useful to have them here but maybe also
    // worth considering getting rid of the duplication. Still,
    // timestamps and log messages are not supposed to ever be
    // changed, in theory...
    public final String TIMESTAMP = "timestamp";
    public final String LOG_MESSAGE = "logMessage";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    @Fetch
    @Adjacency(label = ActionManager.HAS_EVENT_ACTION)
    public Action getAction();

    @Fetch(ifDepth = 0)
    @GremlinGroovy("_().as('e').in('" + ActionManager.LIFECYCLE_EVENT
            + "').loop('e'){true}{it.object.in('"
            + ActionManager.LIFECYCLE_EVENT + "').count()==0}")
    public Iterable<AccessibleEntity> getSubject();

    @Adjacency(label = HAS_PRIOR_EVENT, direction = Direction.IN)
    public ActionEvent getPriorActionEvent();

    @Adjacency(label = HAS_PRIOR_EVENT)
    public ActionEvent getLaterActionEvent();
}
