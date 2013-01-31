package eu.ehri.project.persistance;

import java.util.Iterator;
import java.util.UUID;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.SystemEventQueue;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;

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
     * Because fetching the user and/or subjects from an Event
     * potentially means traversing several links in the item/user
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

    private final FramedGraph graph;
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
     * EventContext is a handle to a particular action to which additional
     * subjects can be added.
     *
     * @author mike
     */
    public static class EventContext {
        private final ActionManager actionManager;
        private final SystemEvent systemEvent;
        private final Actioner actioner;
        private final String logMessage;

        public EventContext(ActionManager actionManager, SystemEvent systemEvent,
                Actioner actioner, String logMessage) {
            this.actionManager = actionManager;
            this.systemEvent = systemEvent;
            this.actioner = actioner;
            this.logMessage = logMessage;
        }

        public SystemEvent getSystemEvent() {
            return this.systemEvent;
        }

        public Actioner getActioner() {
            return this.actioner;
        }

        public EventContext addSubjects(AccessibleEntity... entities) {
            for (AccessibleEntity entity : entities) {
                Vertex vertex = actionManager.graph.addVertex(null);
                actionManager.replaceAtHead(entity.asVertex(), vertex,
                        LIFECYCLE_EVENT, LIFECYCLE_EVENT, Direction.OUT);
                actionManager.graph.addEdge(null, vertex,
                        systemEvent.asVertex(), eu.ehri.project.models.events.SystemEvent.HAS_EVENT);
            }
            return this;
        }
    }

    public SystemEvent getLatestGlobalEvent() {
        try {
            SystemEventQueue sys = manager.getFrame(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM, SystemEventQueue.class);
            Iterable<SystemEvent> latest = sys.getSystemEvents();
            return latest.iterator().hasNext() ? latest.iterator().next() : null;
        } catch (ItemNotFound itemNotFound) {
            throw new RuntimeException("Fatal error: system node (id: 'system') was not found. " +
                    "Perhaps the graph was incorrectly initialised?");
        }
    }

    private PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> noOpBooleanFunction() {
        return new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
            @Override
            public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                return true;
            }
        };
    }

    public Iterable<SystemEvent> getLatestGlobalEvents() {
        SystemEvent systemEvent = getLatestGlobalEvent();
//        GremlinPipeline startPipe = new GremlinPipeline<Vertex, Vertex>(systemEvent.asVertex())
//                .out(LIFECYCLE_ACTION + "Stream");
//        GremlinPipeline pipe = startPipe.copySplit(
//                new GremlinPipeline<Vertex, Vertex>()._(),
//                new GremlinPipeline<Vertex, Vertex>(systemEvent.asVertex())
//                        .out(LIFECYCLE_ACTION + "Stream")._().as("n")
//                        .out(LIFECYCLE_ACTION)
//                        .loop("n", noOpBooleanFunction(), noOpBooleanFunction()));

        try {
            SystemEventQueue queue = manager.getFrame(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM, SystemEventQueue.class);
            return queue.getSystemEvents();
        } catch (ItemNotFound itemNotFound) {
            throw new RuntimeException("Couldn't find system event queue!");
        }
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
    private SystemEvent createGlobalEvent(Actioner user, String actionType, String logMessage) {
        try {
            Vertex system = manager.getVertex(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM);
            Bundle ge = new Bundle(EntityClass.SYSTEM_EVENT)
                    .withDataValue(eu.ehri.project.models.events.SystemEvent.TIMESTAMP, getTimestamp())
                    .withDataValue(SystemEvent.LOG_MESSAGE, logMessage)
                    .withDataValue(AccessibleEntity.IDENTIFIER_KEY, UUID.randomUUID().toString())
                    .withDataValue(USER_ID_CACHE, manager.getId(user));
            SystemEvent ev = new BundleDAO(graph).create(ge, SystemEvent.class);
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
    public EventContext logEvent(Actioner user, String logMessage) {
        Vertex vertex = graph.addVertex(null);
        replaceAtHead(user.asVertex(), vertex, LIFECYCLE_ACTION, LIFECYCLE_ACTION, Direction.OUT);
        SystemEvent ge = createGlobalEvent(user, LIFECYCLE_ACTION, logMessage);
        graph.addEdge(null, vertex, ge.asVertex(), eu.ehri.project.models.events.SystemEvent.HAS_EVENT);
        return new EventContext(this, ge, user, logMessage);
    }

    /**
     * Create an action given an accessor.
     */
//    public EventContext logEvent(AccessibleEntity subject, Accessor user,
//            String logMessage) {
//        return logEvent(subject, graph.frame(user.asVertex(), Actioner.class), logMessage);
//    }

    /**
     * Create an action node that describes what user U has done with subject S
     * via logMessage log.
     *
     * @param subject
     * @param user
     * @param logMessage
     * @return
     */
    public EventContext logEvent(AccessibleEntity subject, Actioner user,
            String logMessage) {
        EventContext context = logEvent(user, logMessage);
        context.addSubjects(subject);
        return context;
    }


    // Helpers.

    public static String getTimestamp() {
        DateTime dt = DateTime.now();
        return ISODateTimeFormat.dateTime().print(dt);
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
