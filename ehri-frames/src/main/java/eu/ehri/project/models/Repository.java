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


/**
 * A frame class for graph nodes representing repository
 * items.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.REPOSITORY)
public interface Repository extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity, PermissionScope, ItemHolder, Watchable {

    /**
     * Count the number of top-level documentary unit items within
     * this repository.
     *
     * @return the number of top-level items
     */
    @JavaHandler
    public long getChildCount();

    /**
     * Fetch all top-level documentary unit items within this
     * repository.
     *
     * @return an iterable of top-level items
     */
    @Adjacency(label = Ontology.DOC_HELD_BY_REPOSITORY, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getCollections();

    /**
     * Fetch items at <b>all</b> levels (including children of top-level
     * items and their children, recursively.)
     *
     * @return an iterable of documentary unit items
     */
    @JavaHandler
    public Iterable<DocumentaryUnit> getAllCollections();

    /**
     * Add a documentary unit as a top-level item in this
     * repository.
     *
     * @param unit a documentary unit item
     */
    @JavaHandler
    public void addCollection(final DocumentaryUnit unit);

    /**
     * Fetch the country in which this repository resides.
     *
     * @return a country frame
     */
    @Fetch(Ontology.REPOSITORY_HAS_COUNTRY)
    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.OUT)
    public Iterable<Country> getCountry();

    /**
     * The the country in which this repository resides.
     *
     * @param country a country frame
     */
    @JavaHandler
    public void setCountry(final Country country);

    /**
     * Update/reset the cache for the number of documentary unit
     * items within this repository.
     */
    @JavaHandler
    public void updateChildCountCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Repository {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).count());
        }

        public long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).count();
            }
            return count;
        }

        public void addCollection(final DocumentaryUnit unit) {
            if (JavaHandlerUtils.addSingleRelationship(unit.asVertex(), it(),
                    Ontology.DOC_HELD_BY_REPOSITORY)) {
                updateChildCountCache();
            }
        }

        public void setCountry(final Country country) {
            country.addRepository(frame(it(), Repository.class));
        }

        public Iterable<DocumentaryUnit> getAllCollections() {
            Pipeline<Vertex, Vertex> otherPipe = gremlin().as("n").in(Ontology.DOC_IS_CHILD_OF)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);

            return frameVertices(gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).cast(Vertex.class).copySplit(gremlin(), otherPipe)
                    .fairMerge().cast(Vertex.class));
        }
    }
}
