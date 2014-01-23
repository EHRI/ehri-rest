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
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

@EntityType(EntityClass.REPOSITORY)
public interface Repository extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity, PermissionScope, ItemHolder, Watchable {

    @JavaHandler
    public Long getChildCount();

    @Adjacency(label = Ontology.DOC_HELD_BY_REPOSITORY, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getCollections();

    @JavaHandler
    public Iterable<DocumentaryUnit> getAllCollections();

    @JavaHandler
    public void addCollection(final DocumentaryUnit collection);

    @Fetch(Ontology.REPOSITORY_HAS_COUNTRY)
    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.OUT)
    public Iterable<Country> getCountry();

    @JavaHandler
    public void setCountry(final Country country);

    @JavaHandler
    public void updateChildCountCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Repository {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).count());
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).count();
            }
            return count;
        }

        public void addCollection(final DocumentaryUnit collection) {
            if (JavaHandlerUtils.addSingleRelationship(collection.asVertex(), it(),
                    Ontology.DOC_HELD_BY_REPOSITORY)) {
                updateChildCountCache();
            }
        }

        public void setCountry(final Country country) {
            country.addRepository(frame(it(), Repository.class));
        }

        public Iterable<DocumentaryUnit> getAllCollections() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }
    }
}
