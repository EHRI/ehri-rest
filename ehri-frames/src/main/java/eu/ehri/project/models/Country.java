package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * User: michaelb
 *
 * Frame class representing a country. It's identifier should
 * be represented by an ISO3166 Alpha 2 code, lower cased.
 */
@EntityType(EntityClass.COUNTRY)
public interface Country extends IdentifiableEntity, AccessibleEntity,
        PermissionScope, ItemHolder {

    public static final String COUNTRY_CODE = Ontology.IDENTIFIER_KEY;

    /**
     * Alias function for fetching the country code identifier.
     * @return
     */
    @Mandatory
    @Property(COUNTRY_CODE)
    public String getCode();

    @JavaHandler
    public Long getChildCount();

    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN)
    public Iterable<Repository> getRepositories();

    @JavaHandler
    public void addRepository(final Repository repository);

    @JavaHandler
    public void updateChildCountCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Country {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.REPOSITORY_HAS_COUNTRY).count());
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.DOC_HELD_BY_REPOSITORY).count();
            }
            return count;
        }

        public void addRepository(final Repository repository) {
            if (JavaHandlerUtils.addSingleRelationship(repository.asVertex(), it(),
                    Ontology.REPOSITORY_HAS_COUNTRY)) {
                updateChildCountCache();
            }
        }
    }
}
