package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerImpl;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AccessibleEntity,
        DescribedEntity, PermissionScope {

    public static final String CHILD_OF = "childOf";

    /**
     * Get the repository that holds this documentary unit.
     * @return
     */
    @Fetch(Repository.HELD_BY)
    @JavaHandler
    public Repository getRepository();

    /**
     * Set the repository that holds this documentary unit.
     * @param institution
     */
    @Adjacency(label = Repository.HELD_BY)
    public void setRepository(final Repository institution);

    /**
     * Get parent documentary unit, if any
     * @return
     */
    @Fetch(CHILD_OF)
    @Adjacency(label = CHILD_OF)
    public DocumentaryUnit getParent();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public void addChild(final DocumentaryUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getAncestors();

    /**
     * Get child documentary units
     * @return
     */
    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getChildren();

    @Adjacency(label = DescribedEntity.DESCRIBES, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerImpl<Vertex>, DocumentaryUnit {
        public Repository getRepository() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").out(CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.OUT,
                                    CHILD_OF).iterator().hasNext();
                        }
                    });

            GremlinPipeline<Vertex,Vertex> out = gremlin().cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .exhaustMerge().out(Repository.HELD_BY);

            return (Repository)(out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<DocumentaryUnit> getAncestors() {
            return frameVertices(gremlin().as("n")
                    .out(CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
