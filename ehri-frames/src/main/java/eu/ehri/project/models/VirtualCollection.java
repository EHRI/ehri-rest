package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.VIRTUAL_COLLECTION)
public interface VirtualCollection extends AccessibleEntity,
        DescribedEntity, PermissionScope, ItemHolder {

    @JavaHandler
    public Long getChildCount();

    /**
     * Get parent documentary unit, if any
     * @return
     */
    @Fetch(Ontology.VC_IS_PART_OF)
    @Adjacency(label = Ontology.VC_IS_PART_OF)
    public VirtualCollection getParent();

    @JavaHandler
    public void addChild(final VirtualCollection child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @JavaHandler
    public Iterable<VirtualCollection> getAncestors();

    /**
     * Get child documentary units
     * @return
     */
    @JavaHandler
    public Iterable<VirtualCollection> getChildren();

    @JavaHandler
    public Iterable<VirtualCollection> getAllChildren();

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, VirtualCollection {

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                it().setProperty(CHILD_COUNT, gremlin().in(Ontology.VC_IS_PART_OF).count());
            }
            return count;
        }

        public Iterable<VirtualCollection> getChildren() {
            // Ensure value is cached when fetching.
            getChildCount();
            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF));
        }

        public void addChild(final VirtualCollection child) {
            child.asVertex().addEdge(Ontology.VC_IS_PART_OF, it());
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                getChildCount();
            } else {
                it().setProperty(CHILD_COUNT, count + 1);
            }
        }

        public Iterable<VirtualCollection> getAllChildren() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").in(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }

        public Iterable<VirtualCollection> getAncestors() {
            return frameVertices(gremlin().as("n")
                    .out(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc));
        }
    }
}
