package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

import static eu.ehri.project.models.utils.JavaHandlerUtils.addSingleRelationship;
import static eu.ehri.project.models.utils.JavaHandlerUtils.addUniqueRelationship;

/**
 * Virtual documentary unit. Note: a *virtual* unit can
 * have its own descriptions which do not refer to *actual*
 * doc units, but are structurally the same. However, the label
 * and direction is different in these cases, with the "purely
 * virtual" descriptions having an outgoing "describes" relationship
 * to the VU, whereas descriptions that describe real doc units have
 * an incoming "isDescribedBy" relationship from a VU. The difference
 * denotes ownership (dependency) which likewise controls cascading
 * deletions.
 */
@EntityType(EntityClass.VIRTUAL_UNIT)
public interface VirtualUnit extends AccessibleEntity, ItemHolder,
        DescribedEntity, Watchable {

    @JavaHandler
    public Long getChildCount();

    @Fetch(Ontology.VC_IS_PART_OF)
    @Adjacency(label = Ontology.VC_IS_PART_OF)
    public VirtualUnit getParent();

    /**
     * Add a child. Note: this should throw an exception like
     * IllegalEdgeLoop if the operation is self-referential or
     * results in a loop, but due to a frames limitation we can't
     * Instead it returns a boolean indicating success/failure.
     *
     * @param child The child collection
     * @return Whether or not the operation was allowed.
     */
    @JavaHandler
    public boolean addChild(final VirtualUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @JavaHandler
    public Iterable<VirtualUnit> getAncestors();

    @JavaHandler
    public Iterable<VirtualUnit> getChildren();

    @JavaHandler
    public Iterable<VirtualUnit> getAllChildren();

    @Fetch(Ontology.VC_DESCRIBED_BY)
    @Adjacency(label = Ontology.VC_DESCRIBED_BY, direction = Direction.OUT)
    public Iterable<DocumentDescription> getReferencedDescriptions();

    @JavaHandler
    public void addReferencedDescription(final DocumentDescription description);

    @Adjacency(label = Ontology.VC_HAS_AUTHOR, direction = Direction.OUT)
    public Accessor getAuthor();

    @JavaHandler
    public void setAuthor(final Accessor accessor);

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public Iterable<DocumentDescription> getVirtualDescriptions();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, VirtualUnit {

        public void setAuthor(final Accessor accessor) {
            addSingleRelationship(it(), accessor.asVertex(), Ontology.VC_HAS_AUTHOR);
        }

        public void addReferencedDescription(final DocumentDescription description) {
            addUniqueRelationship(it(), description.asVertex(), Ontology.VC_DESCRIBED_BY);
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.VC_IS_PART_OF).count();
            }
            return count;
        }

        public Iterable<VirtualUnit> getChildren() {
            // Ensure value is cached when fetching.
            getChildCount();
            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF));
        }

        public boolean addChild(final VirtualUnit child) {
            if (child.asVertex().equals(it())) {
                // Self-referential.
                return false;
            }
            for (Vertex parent : traverseAncestors()) {
                if (child.equals(parent)) {
                    // Loop
                    return false;
                }
            }

            boolean done = addUniqueRelationship(child.asVertex(), it(), Ontology.VC_IS_PART_OF);
            if (done) {
                Long count = it().getProperty(CHILD_COUNT);
                if (count == null) {
                    it().setProperty(CHILD_COUNT, gremlin().in(Ontology.VC_IS_PART_OF).count());
                } else {
                    it().setProperty(CHILD_COUNT, count + 1);
                }
            }
            return done;
        }

        private GremlinPipeline<Vertex, Vertex> traverseAncestors() {
            return gremlin().as("n")
                    .out(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, JavaHandlerUtils.noopLoopFunc);
        }

        public Iterable<VirtualUnit> getAllChildren() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.VC_IS_PART_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.VC_IS_PART_OF).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }

        public Iterable<VirtualUnit> getAncestors() {
            return frameVertices(traverseAncestors());
        }
    }
}
