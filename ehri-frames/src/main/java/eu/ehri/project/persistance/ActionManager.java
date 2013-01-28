package eu.ehri.project.persistance;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.ActionEvent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;

/**
 * Class for dealing with actions.
 * 
 * @author michaelb
 * 
 */
public final class ActionManager {

    /**
     * Because fetching the user and/or subjects from an Action or an
     * ActionEvent potentially means traversing several links in the item/user
     * history, we can their IDs as a property.
     */
    private static final String USER_ID_CACHE = "__USER_ID_CACHE__";
    @SuppressWarnings("unused")
    private static final String ITEM_ID_CACHE = "__ITEM_ID_CACHE__";

    /**
     * Constant relationship names
     */
    public static final String LIFECYCLE_ACTION = "lifecycleAction";
    public static final String INTERACTION_ACTION = "interactionAction";
    public static final String LIFECYCLE_EVENT = "lifecycleEvent";
    public static final String INTERACTION_EVENT = "interactionEvent";
    public static final String HAS_EVENT_ACTION = "hasEventAction";

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;

    /**
     * Constructor.
     * 
     * @param graph
     */
    public ActionManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Create an action node describing something that user U has done.
     * 
     * @param user
     * @param logMessage
     * @return
     */
    public Action createAction(Actioner user, String logMessage) {
        Bundle actionBundle = new Bundle(EntityClass.ACTION)
                .withDataValue(Action.TIMESTAMP, getTimestamp())
                .withDataValue(Action.LOG_MESSAGE, logMessage)
                .withDataValue(USER_ID_CACHE, manager.getId(user))
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY,
                        UUID.randomUUID().toString());

        BundleDAO persister = new BundleDAO(graph, null);
        try {
            Action action = persister.create(actionBundle, Action.class);
            setLatestAction(user, action, LIFECYCLE_ACTION);
            return action;
        } catch (ValidationError e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unexpected validation error creating action", e);
        }
    }

    /**
     * Create an action node describing something that user U has done.
     * 
     * @param user
     * @param logMessage
     * @return
     */
    public ActionEvent createEvent(AccessibleEntity item, Actioner user,
            String logMessage) {
        Bundle actionBundle = new Bundle(EntityClass.ACTION_EVENT)
                .withDataValue(Action.TIMESTAMP, getTimestamp())
                .withDataValue(Action.LOG_MESSAGE, logMessage)
                .withDataValue(USER_ID_CACHE, manager.getId(user))
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY,
                        UUID.randomUUID().toString());
        BundleDAO persister = new BundleDAO(graph, null);
        try {
            ActionEvent action = persister.create(actionBundle,
                    ActionEvent.class);
            setLatestEvent(item, action, LIFECYCLE_EVENT);
            return action;
        } catch (ValidationError e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unexpected validation error creating action", e);
        }
    }

    /**
     * Add a new action to the head of this user's action history linked list.
     * 
     * @param user
     * @param action
     */
    private void setLatestAction(Actioner user, Action action, String actionType) {
        Action existingAction = user.getLatestAction();
        if (existingAction != null) {
            for (Edge e : user.asVertex().getEdges(Direction.OUT,
                    actionType)) {
                graph.removeEdge(e);
            }
            graph.addEdge(null, action.asVertex(), existingAction.asVertex(),
                    actionType);
        }
        graph.addEdge(null, user.asVertex(), action.asVertex(), actionType);
    }

    /**
     * Add a new event to the head of this item's event history linked list.
     * 
     * @param item
     * @param action
     */
    private void setLatestEvent(AccessibleEntity item, ActionEvent event, String actionType) {
        ActionEvent existingEvent = item.getLatestEvent();
        if (existingEvent != null) {
            for (Edge e : item.asVertex().getEdges(Direction.OUT,
                    actionType)) {
                graph.removeEdge(e);
            }
            graph.addEdge(null, event.asVertex(), existingEvent.asVertex(),
                    actionType);
        }
        graph.addEdge(null, item.asVertex(), event.asVertex(), actionType);
    }

    /**
     * Create an action node that describes what user U has done with subject S
     * via logMessage log.
     * 
     * @param subject
     * @param user
     * @param logMessage
     * @return
     */
    public Action createAction(AccessibleEntity subject, Actioner user,
            String logMessage) {
        Action action = createAction(user, logMessage);
        ActionEvent event = createEvent(subject, user, logMessage);
        graph.addEdge(null, event.asVertex(), action.asVertex(), HAS_EVENT_ACTION);
        return action;
    }
    
    /**
     * Add subjects to an existing action.
     * @return
     */
    public void addSubjects(Action action, Actioner user, AccessibleEntity... entities) {
        String logMsg = action.getLogMessage();
        for (AccessibleEntity entity :  entities) {
            ActionEvent event = createEvent(entity, user, logMsg);
            graph.addEdge(null, event.asVertex(), action.asVertex(), HAS_EVENT_ACTION);
        }
    }

    // Helpers.

    private String getTimestamp() {
        DateTime dt = DateTime.now();
        return ISODateTimeFormat.dateTime().print(dt);
    }
}
