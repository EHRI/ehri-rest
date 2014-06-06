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

@EntityType(EntityClass.VERSION)
public interface Version extends AccessibleEntity {

    @Mandatory
    @Property(Ontology.VERSION_ENTITY_CLASS)
    public EntityClass getEntityClass();

    @Mandatory
    @Property(Ontology.VERSION_ENTITY_ID)
    public String getEntityId();

    /**
     * Serialized snapshot of the item's data.
     */
    @Mandatory
    @Property(Ontology.VERSION_ENTITY_DATA)
    public String getEntityData();

    /**
     * The event that triggered this version.
     */
    @Fetch(value = Ontology.VERSION_HAS_EVENT, ifDepth = 0)
    @Adjacency(label = Ontology.VERSION_HAS_EVENT, direction = Direction.OUT)
    public SystemEvent getTriggeringEvent();

    @Adjacency(label = Ontology.VERSION_HAS_EVENT, direction = Direction.OUT)
    public void setTriggeringEvent(final SystemEvent event);

    @JavaHandler
    public AccessibleEntity getEntity();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Version {

        /**
         * Loops up through the chain of hasPriorVersion until the end, until it
         * has been deleted, will be a item.
         * @return
         */
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
