package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;

import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.REPOSITORY)
public interface Repository extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity, PermissionScope {

    public static final String HELD_BY = "heldBy";
    public static final String HAS_COUNTRY = "hasCountry";

    @Adjacency(label = HELD_BY, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getCollections();

    @JavaHandler
    public Iterable<DocumentaryUnit> getAllCollections();

    @Adjacency(label = HELD_BY, direction = Direction.IN)
    public void addCollection(final TemporalEntity collection);

    @Fetch(HAS_COUNTRY)
    @Adjacency(label = HAS_COUNTRY, direction = Direction.OUT)
    public Iterable<Country> getCountry();

    @Adjacency(label = HAS_COUNTRY, direction = Direction.OUT)
    public void setCountry(final Country country);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Repository {
        public Iterable<DocumentaryUnit> getAllCollections() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").in(DocumentaryUnit.CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(HELD_BY).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }
    }
}
