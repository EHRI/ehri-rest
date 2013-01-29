package eu.ehri.project.persistance;

import java.util.Iterator;
import java.util.UUID;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.events.GlobalEvent;
import eu.ehri.project.models.events.ItemEvent;
import eu.ehri.project.views.impl.Query;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.events.Action;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * Class for dealing with actions.
 *
 * @author michaelb
 */
public final class ActionManager {

    // Name of the global event root node, from whence event
    // streams propagate.
    public static final String GLOBAL_EVENT_ROOT = "globalEventRoot";

    /**
     * Because fetching the user and/or subjects from an Action or an
     * ItemEvent potentially means traversing several links in the item/user
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
     * ActionContext is a handle to a particular action to which additional
     * subjects can be added.
     *
     * @author mike
     */
    public static class ActionContext {
        private final ActionManager actionManager;
        private final Action action;
        private final GlobalEvent globalEvent;
        private final Actioner actioner;
        private final String logMessage;

        public ActionContext(ActionManager actionManager, Action action, GlobalEvent globalEvent,
                Actioner actioner, String logMessage) {
            this.actionManager = actionManager;
            this.action = action;
            this.globalEvent = globalEvent;
            this.actioner = actioner;
            this.logMessage = logMessage;
        }

        public Action getAction() {
            return this.action;
        }

        public GlobalEvent getGlobalEvent() {
            return this.globalEvent;
        }

        public Actioner getActioner() {
            return this.actioner;
        }

        public ActionContext addSubjects(AccessibleEntity... entities) {
            for (AccessibleEntity entity : entities) {
                ItemEvent event = actionManager.createEvent(entity, actioner,
                        logMessage);
                actionManager.graph.addEdge(null, event.asVertex(),
                        action.asVertex(), Action.HAS_EVENT_ACTION);
                actionManager.graph.addEdge(null, event.asVertex(),
                        globalEvent.asVertex(), ItemEvent.HAS_GLOBAL_EVENT);
            }
            return this;
        }
    }

    public GlobalEvent getLatestGlobalEvent() {
        try {
            Vertex sys = manager.getVertex(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM);
            Iterable<Vertex> latest = sys.getVertices(Direction.OUT, LIFECYCLE_ACTION + "Stream");
            return latest.iterator().hasNext() ? graph.frame(latest.iterator().next(), GlobalEvent.class) : null;
        } catch (ItemNotFound itemNotFound) {
            throw new RuntimeException("Fatal error: system node (id: 'system') was not found. " +
                    "Perhaps the graph was incorrectly initialised?");
        }
    }

    private PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> noopBooleanFunction() {
        return new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
            @Override
            public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                return true;
            }
        };
    }

    public Iterable<GlobalEvent> getLatestGlobalEvents() {
        GlobalEvent globalEvent = getLatestGlobalEvent();
        GremlinPipeline<Vertex, Vertex> loop = new GremlinPipeline<Vertex, Vertex>(globalEvent.asVertex())._()
                .as("n").out(LIFECYCLE_ACTION)
                .loop("n", noopBooleanFunction(), noopBooleanFunction());
        return graph.frameVertices(new GremlinPipeline<Vertex, Vertex>(globalEvent.asVertex())._().and(loop), GlobalEvent.class);
    }

    /**
     * Create a global event and insert it at the head of the system queue. The
     * relationship from the <em>system</em> node to the new latest action is
     * <em>actionType</em><strong>Stream</strong>.
     *
     * @param user
     * @param actionType
     * @param logMessage
     * @return
     */
    public GlobalEvent createGlobalEvent(Actioner user, String actionType, String logMessage) {
        try {
            Vertex system = manager.getVertex(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM);
            Bundle ge = new Bundle(EntityClass.GLOBAL_EVENT)
                    .withDataValue(GlobalEvent.TIMESTAMP, getTimestamp())
                    .withDataValue(GlobalEvent.LOG_MESSAGE, logMessage)
                    .withDataValue(AccessibleEntity.IDENTIFIER_KEY, UUID.randomUUID().toString())
                    .withDataValue(USER_ID_CACHE, manager.getId(user));
            GlobalEvent ev = new BundleDAO(graph).create(ge, GlobalEvent.class);
            replaceAtHead(system, ev.asVertex(), actionType + "Stream", actionType, Direction.OUT);
            return ev;
        } catch (ItemNotFound e) {
            e.printStackTrace();
            throw new RuntimeException("Fatal error: system node (id: 'system') was not found. " +
                    "Perhaps the graph was incorrectly initialised?");
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
    public ActionContext createAction(Actioner user, String logMessage) {
        GlobalEvent ge = createGlobalEvent(user, LIFECYCLE_ACTION, logMessage);
        Bundle actionBundle = new Bundle(EntityClass.ACTION)
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY,
                        UUID.randomUUID().toString());

        BundleDAO persister = new BundleDAO(graph);
        try {
            Action action = persister.create(actionBundle, Action.class);
            setLatestAction(user, action, LIFECYCLE_ACTION);
            graph.addEdge(null, action.asVertex(), ge.asVertex(), Action.HAS_GLOBAL_EVENT);
            return new ActionContext(this, action, ge, user, logMessage);
        } catch (ValidationError e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unexpected validation error creating action", e);
        }
    }

    /**
     * Create an action given an accessor.
     */
    public ActionContext createAction(AccessibleEntity subject, Accessor user,
            String logMessage) {
        return createAction(subject, graph.frame(user.asVertex(), Actioner.class), logMessage);
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
    public ActionContext createAction(AccessibleEntity subject, Actioner user,
            String logMessage) {
        ActionContext context = createAction(user, logMessage);
        context.addSubjects(subject);
        return context;
    }


    // Helpers.

    /**
     * Create an action node describing something that user U has done.
     *
     * @param user
     * @param logMessage
     * @return
     */
    private ItemEvent createEvent(AccessibleEntity item, Actioner user,
            String logMessage) {
        Bundle actionBundle = new Bundle(EntityClass.ACTION_EVENT)
                .withDataValue(AccessibleEntity.IDENTIFIER_KEY,
                        UUID.randomUUID().toString());
        BundleDAO persister = new BundleDAO(graph, null);
        try {
            ItemEvent action = persister.create(actionBundle,
                    ItemEvent.class);
            setLatestEvent(item, action, LIFECYCLE_EVENT);
            return action;
        } catch (ValidationError e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unexpected validation error creating action", e);
        }
    }

    public static String getTimestamp() {
        DateTime dt = DateTime.now();
        return ISODateTimeFormat.dateTime().print(dt);
    }

    /**
     * Add a new action to the head of this user's action history linked list.
     *
     * @param user
     * @param action
     */
    private void setLatestAction(Actioner user, Action action, String actionType) {
        replaceAtHead(user.asVertex(), action.asVertex(), actionType, actionType, Direction.OUT);
    }

    /**
     * Add a new event to the head of this item's event history linked list.
     *
     * @param item
     * @param event
     * @param actionType
     */
    private void setLatestEvent(AccessibleEntity item, ItemEvent event,
            String actionType) {
        replaceAtHead(item.asVertex(), event.asVertex(), actionType, actionType, Direction.OUT);
    }

    /**
     * Given a vertex <em>head</em> that forms that start of a chain <em>relation</em> with
     * direction <em>direction</em>, insert vertex <em>insert</em> <strong>after</strong>
     * the head of the chain.
     *
     * @param head
     * @param newHead
     * @param relation
     * @param direction
     */
    private void replaceAtHead(Vertex head, Vertex newHead, String headRelation, String relation, Direction direction) {
        Iterator<Vertex> iter = head.getVertices(direction, headRelation).iterator();
        if (iter.hasNext()) {
            Vertex current = iter.next();
            for (Edge e : head.getEdges(direction, headRelation)) {
                graph.removeEdge(e);
            }
            graph.addEdge(null, newHead, current, relation);

        }
        graph.addEdge(null, head, newHead, headRelation);
    }
}
