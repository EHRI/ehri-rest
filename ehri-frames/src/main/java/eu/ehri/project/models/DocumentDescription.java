package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.DOCUMENT_DESCRIPTION)
public interface DocumentDescription extends TemporalEntity, Description {

    @Fetch(value = Ontology.VC_IN_COLLECTION, depth = 3)
    @JavaHandler
    public Iterable<VirtualUnit> getVirtualCollections();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, DocumentDescription {

        public Iterable<VirtualUnit> getVirtualCollections() {
            // This is kind of difficult. We want to get the
            Pipeline<Vertex,Vertex> otherPipe = gremlin().in(Ontology.VC_DESCRIBED_BY).as("n").out(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject()
                                    .getEdges(Direction.OUT, Ontology.VC_IS_PART_OF)
                                    .iterator().hasNext();
                        }
                    });

            GremlinPipeline<Vertex,Vertex> out = gremlin().cast(Vertex.class)
                    .copySplit(gremlin().in(Ontology.VC_DESCRIBED_BY), otherPipe)
                    .exhaustMerge().cast(Vertex.class).filter(new PipeFunction<Vertex, Boolean>() {
                        @Override
                        public Boolean compute(Vertex vertex) {
                            return !vertex.getEdges(Direction.OUT, Ontology.VC_IS_PART_OF)
                                    .iterator().hasNext();
                        }
                    });

            return frameVertices(out);

        }
    }
}
