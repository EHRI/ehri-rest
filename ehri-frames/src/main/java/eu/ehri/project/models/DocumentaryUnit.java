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
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AccessibleEntity,
        DescribedEntity, PermissionScope, ItemHolder, Watchable {

    static final Logger logger = LoggerFactory.getLogger(DocumentaryUnit.class);

    /**
     * Get the repository that holds this documentary unit.
     * @return
     */
    @Fetch(Ontology.DOC_HELD_BY_REPOSITORY)
    @JavaHandler
    public Repository getRepository();

    /**
     * Set the repository that holds this documentary unit.
     * @param institution
     */
    @JavaHandler
    public void setRepository(final Repository institution);

    /**
     * Get parent documentary unit, if any
     * @return
     */
    @Fetch(Ontology.DOC_IS_CHILD_OF)
    @Adjacency(label = Ontology.DOC_IS_CHILD_OF)
    public DocumentaryUnit getParent();

    @JavaHandler
    public void addChild(final DocumentaryUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getAncestors();

    @JavaHandler
    public Long getChildCount();

    /**
     * Get child documentary units
     * @return
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getChildren();

    @JavaHandler
    public Iterable<DocumentaryUnit> getAllChildren();

    @JavaHandler
    public void updateChildCountCache();

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, DocumentaryUnit {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.DOC_IS_CHILD_OF).count());
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.DOC_IS_CHILD_OF).count();
            }
            return count;
        }

        public Iterable<DocumentaryUnit> getChildren() {
            return frameVertices(gremlin().in(Ontology.DOC_IS_CHILD_OF));
        }

        public void addChild(final DocumentaryUnit child) {
            if (JavaHandlerUtils
                    .addSingleRelationship(child.asVertex(), it(), Ontology.DOC_IS_CHILD_OF)) {
                updateChildCountCache();
            }
        }

        public Iterable<DocumentaryUnit> getAllChildren() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_IS_CHILD_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }


        public void setRepository(final Repository institution) {
            // NB: Convenience methods that proxies addCollection (which
            // in turn maintains the child item cache.)
            institution.addCollection(frame(it(), DocumentaryUnit.class));
        }

        public Repository getRepository() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return !vertexLoopBundle.getObject().getVertices(Direction.OUT,
                                    Ontology.DOC_IS_CHILD_OF).iterator().hasNext();
                        }
                    });

            GremlinPipeline<Vertex,Vertex> out = gremlin().cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .exhaustMerge().out(Ontology.DOC_HELD_BY_REPOSITORY);

            return (Repository)(out.hasNext() ? frame(out.next()) : null);
        }

        public Iterable<DocumentaryUnit> getAncestors() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
