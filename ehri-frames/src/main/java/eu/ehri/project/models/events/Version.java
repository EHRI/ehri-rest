package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing a serialized version of
 * some other node.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.VERSION)
public interface Version extends AccessibleEntity {

    /**
     * Fetch the class of the entity that this version pertains to.
     *
     * @return an entity class enum value
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_CLASS)
    public String getEntityType();

    /**
     * Fetch the ID of the entity that this version pertains to.
     *
     * @return an ID string
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_ID)
    public String getEntityId();

    /**
     * Fetch a serialized snapshot of the item's data in JSON format.
     *
     * @return JSON data representing a sub-graph
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_DATA)
    public String getEntityData();

    /**
     * Fetch the event that triggered this version.
     *
     * @return a system event instance
     */
    @Fetch(value = Ontology.VERSION_HAS_EVENT, ifDepth = 0)
    @Adjacency(label = Ontology.VERSION_HAS_EVENT, direction = Direction.OUT)
    public SystemEvent getTriggeringEvent();

    /**
     * Loops up through the chain of versions until the latest and fetches
     * the item to which the events refer. If it has been
     *
     * @return the entity to which this version refers.
     */
    @JavaHandler
    public AccessibleEntity getEntity();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Version {
        public AccessibleEntity getEntity() {
            Pipeline<Vertex,Vertex> out =  gremlin().as("n").in(Ontology.ENTITY_HAS_PRIOR_VERSION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.IN,
                                    Ontology.ENTITY_HAS_PRIOR_VERSION).iterator().hasNext();
                        }
                    });
            return (AccessibleEntity)(out.hasNext() ? frame(out.next()) : null);
        }
    }
}
