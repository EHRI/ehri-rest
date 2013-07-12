package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AccessibleEntity,
        DescribedEntity, PermissionScope, ItemHolder {

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
    @JavaHandler
    public void setRepository(final Repository institution);

    @JavaHandler
    public Long getChildCount();

    /**
     * Get parent documentary unit, if any
     * @return
     */
    @Fetch(CHILD_OF)
    @Adjacency(label = CHILD_OF)
    public DocumentaryUnit getParent();

    @JavaHandler
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
    @JavaHandler
    public Iterable<DocumentaryUnit> getChildren();

    @JavaHandler
    public Iterable<DocumentaryUnit> getAllChildren();

    @Adjacency(label = DescribedEntity.DESCRIBES, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, DocumentaryUnit {

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                it().setProperty(CHILD_COUNT, gremlin().in(CHILD_OF).count());
            }
            return count;
        }

        public Iterable<DocumentaryUnit> getChildren() {
            // Ensure value is cached when fetching.
            getChildCount();
            return frameVertices(gremlin().in(CHILD_OF));
        }

        public void addChild(final DocumentaryUnit child) {
            child.asVertex().addEdge(CHILD_OF, it());
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                getChildCount();
            } else {
                it().setProperty(CHILD_COUNT, count + 1);
            }
        }

        public Iterable<DocumentaryUnit> getAllChildren() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").in(CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(CHILD_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }


        public void setRepository(final Repository institution) {
            // NB: Convenience methods that proxies addCollection (which
            // in turn maintains the child item cache.)
            institution.addCollection(frame(it(), DocumentaryUnit.class));
        }

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
