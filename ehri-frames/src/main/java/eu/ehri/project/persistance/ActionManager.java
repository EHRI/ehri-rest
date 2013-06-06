package eu.ehri.project.persistance;

import java.util.Iterator;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.SystemEventQueue;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
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
     * Constant relationship names
     */
    public static final String LIFECYCLE_ACTION = "lifecycleAction";
    public static final String LIFECYCLE_EVENT = "lifecycleEvent";

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Frame scope;

    /**
     * Constructor with scope.
     *
     * @param graph
     */
    public ActionManager(final FramedGraph<?> graph, final Frame scope) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.scope = Optional.fromNullable(scope).or(SystemScope.getInstance());
    }

    /**
     * Constructor.
     *
     * @param graph
     */
    public ActionManager(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
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
        private final EventTypes actionType;
        private final Optional<String> logMessage;

        public EventContext(ActionManager actionManager, SystemEvent systemEvent,
                Actioner actioner, EventTypes type, Optional<String> logMessage) {
            this.actionManager = actionManager;
            this.actionType = type;
            this.systemEvent = systemEvent;
            this.actioner = actioner;
            this.logMessage = logMessage;
        }

        public SystemEvent getSystemEvent() {
            return this.systemEvent;
        }

        /**
         * Get the event actioner.
         * @return
         */
        public Actioner getActioner() {
            return this.actioner;
        }
        
        /**
         * Get the event context log message.
         * @return
         */
        public Optional<String> getLogMessage() {
            return this.logMessage;
        }

        /**
         * Add subjects to an event.
         * @param entities
         * @return
         */
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

    /**
     * Get the latest global event.
     * @return
     */
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

    /**
     * Get an iterable of global events in most-recent-first order.
     * @return
     */
    public Iterable<SystemEvent> getLatestGlobalEvents() {
        try {
            SystemEventQueue queue = manager.getFrame(
                    GLOBAL_EVENT_ROOT, EntityClass.SYSTEM, SystemEventQueue.class);
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
    private SystemEvent createGlobalEvent(Actioner user, EventTypes actionType, Optional<String> logMessage) {
        try {
            Vertex system = manager.getVertex(GLOBAL_EVENT_ROOT, EntityClass.SYSTEM);
            Bundle ge = new Bundle(EntityClass.SYSTEM_EVENT)
                    .withDataValue(SystemEvent.EVENT_TYPE, actionType.toString())
                    .withDataValue(SystemEvent.TIMESTAMP, getTimestamp())
                    .withDataValue(SystemEvent.LOG_MESSAGE, logMessage.or(""));
            SystemEvent ev = new BundleDAO(graph).create(ge, SystemEvent.class);
            if (!scope.equals(SystemScope.getInstance())) {
                ev.setEventScope(scope);
            }
            replaceAtHead(system, ev.asVertex(), LIFECYCLE_ACTION + "Stream", LIFECYCLE_ACTION, Direction.OUT);
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
     * Create an action with the given type.
     * @param user
     * @param type
     * @return
     */
    public EventContext logEvent(Actioner user, EventTypes type) {
        return logEvent(user, type, Optional.<String>absent());
    }

    /**
     * Create an action with the given type and a log message.
     * @param user
     * @param type
     * @param logMessage
     * @return
     */
    public EventContext logEvent(Actioner user, EventTypes type, String logMessage) {
        return logEvent(user, type, Optional.of(logMessage));
    }

    /**
     * Create an action node describing something that user U has done.
     *
     * @param user
     * @param type
     * @param logMessage
     * @return
     */
    public EventContext logEvent(Actioner user, EventTypes type, Optional<String> logMessage) {
        Vertex vertex = graph.addVertex(null);
        replaceAtHead(user.asVertex(), vertex,
                LIFECYCLE_ACTION, LIFECYCLE_ACTION, Direction.OUT);
        SystemEvent globalEvent = createGlobalEvent(user, type, logMessage);
        graph.addEdge(null, vertex, globalEvent.asVertex(), SystemEvent.HAS_EVENT);
        return new EventContext(this, globalEvent, user, type, logMessage);
    }

    /**
     * Create an action for the given subject, user, and type.
     * @param subject
     * @param user
     * @param type
     * @return
     */
    public EventContext logEvent(AccessibleEntity subject, Actioner user,
            EventTypes type) {
        return logEvent(subject, user, type, Optional.<String>absent());
    }

    /**
     * Create an action for the given subject, user, and type and a log message.
     * @param subject
     * @param user
     * @param type
     * @param logMessage
     * @return
     */
    public EventContext logEvent(AccessibleEntity subject, Actioner user,
            EventTypes type, String logMessage) {
        return logEvent(subject, user, type, Optional.of(logMessage));
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
    public EventContext logEvent(AccessibleEntity subject, Actioner user,
            EventTypes type, Optional<String> logMessage) {
        EventContext context = logEvent(user, type, logMessage);
        context.addSubjects(subject);
        return context;
    }

    public ActionManager setScope(Frame frame) {
        return new ActionManager(graph,
                Optional.fromNullable(frame).or(SystemScope.getInstance()));
    }


    // Helpers.

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
    private void replaceAtHead(Vertex head, Vertex newHead, String headRelation,
            String relation, Direction direction) {
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

    /**
     * Get the current time as a timestamp.
     * @return
     */
    public static String getTimestamp() {
        DateTime dt = DateTime.now();
        return ISODateTimeFormat.dateTime().print(dt);
    }
}
