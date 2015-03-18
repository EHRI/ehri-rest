package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing an event that happened in the
 * graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.SYSTEM_EVENT)
public interface SystemEvent extends AccessibleEntity {

    @Meta(ItemHolder.CHILD_COUNT)
    @JavaHandler
    public long subjectCount();

    /**
     * Fetch the time stamp of this event.
     *
     * @return a UTF timestamp string
     */
    @Mandatory
    @Property(Ontology.EVENT_TIMESTAMP)
    public String getTimestamp();

    /**
     * Fetch the event type of this event.
     *
     * @return an {@link EventTypes} value
     */
    @Mandatory
    @Property(Ontology.EVENT_TYPE)
    public EventTypes getEventType();

    /**
     * Fetch the log message associated with this event.
     *
     * @return A string
     */
    @Property(Ontology.EVENT_LOG_MESSAGE)
    public String getLogMessage();

    /**
     * Fetch the actioner who triggered this event.
     *
     * @return A user profile instance
     */
    @Fetch(value = Ontology.EVENT_HAS_ACTIONER, numLevels = 0)
    @JavaHandler
    public Actioner getActioner();

    /**
     * Fetch the subject items to whom this event pertains.
     *
     * @return an iterable of frame items
     */
    @JavaHandler
    public Iterable<AccessibleEntity> getSubjects();

    /**
     * If new versions have been created, fetch the prior versions
     * of the subjects that were affected by this event.
     *
     * @return an iterable of version nodes
     */
    @Fetch(value = Ontology.VERSION_HAS_EVENT, ifLevel = 0)
    @Adjacency(label = Ontology.VERSION_HAS_EVENT, direction = Direction.IN)
    public Iterable<Version> getPriorVersions();

    /**
     * Fetch the first subject of this event. This is a shortcut
     * method to avoid having to fetch many items.
     *
     * @return an item frame
     */
    @Fetch(value = Ontology.EVENT_HAS_FIRST_SUBJECT, ifLevel = 0)
    @JavaHandler
    public AccessibleEntity getFirstSubject();

    /**
     * Fetch the "scope" of this event, or the context in which a
     * given creation/modification/deletion event is happening.
     *
     * @return the event scope
     */
    @Fetch(value = Ontology.EVENT_HAS_SCOPE, ifLevel = 0)
    @Adjacency(label = Ontology.EVENT_HAS_SCOPE, direction = Direction.OUT)
    public Frame getEventScope();

    /**
     * Set the scope of this event.
     *
     * @param frame a scope item
     */
    @Adjacency(label = Ontology.EVENT_HAS_SCOPE, direction = Direction.OUT)
    public void setEventScope(final Frame frame);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, SystemEvent {

        @Override
        public long subjectCount() {
            return gremlin().inE(Ontology.ENTITY_HAS_EVENT).count();
        }

        @Override
        public Iterable<AccessibleEntity> getSubjects() {
            return frameVertices(gremlin().in(Ontology.ENTITY_HAS_EVENT)
                    .as("n").in(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return isValidTarget(vertexLoopBundle.getObject());
                        }
                    }));
        }

        @Override
        public AccessibleEntity getFirstSubject() {
            // Ugh: horrible code duplication is horrible - unfortunately
            // just calling getSubjects() fails for an obscure reason to do
            // with Frames not being thinking it has an iterable???
            GremlinPipeline<Vertex,Vertex> subjects = gremlin().in(Ontology.ENTITY_HAS_EVENT)
                    .as("n").in(Ontology.ENTITY_HAS_LIFECYCLE_EVENT)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return isValidTarget(vertexLoopBundle.getObject());
                        }
                    });
            return (AccessibleEntity)(subjects.iterator().hasNext()
                ? frame(subjects.iterator().next())
                : null);
        }

        @Override
        public Actioner getActioner() {
            GremlinPipeline<Vertex, Vertex> actioners = gremlin().in(Ontology.ACTION_HAS_EVENT)
                    .as("n").in(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return isValidActioner(vertexLoopBundle.getObject());
                        }
                    });
            return (Actioner)(actioners.iterator().hasNext()
                    ? frame(actioners.iterator().next())
                    : null);
        }

        private boolean isValidActioner(Vertex vertex) {
            return (!vertex.getVertices(Direction.IN,
                    Ontology.ACTIONER_HAS_LIFECYCLE_ACTION).iterator().hasNext())
                    && vertex.getProperty(EntityType.TYPE_KEY) != null;
        }

        private boolean isValidTarget(Vertex vertex) {
            return (!vertex.getVertices(Direction.IN,
                    Ontology.ENTITY_HAS_LIFECYCLE_EVENT).iterator().hasNext())
                    && vertex.getProperty(EntityType.TYPE_KEY) != null;
        }
    }
}
