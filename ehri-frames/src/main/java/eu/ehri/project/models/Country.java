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
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing a country. It's identifier should
 * be represented by an ISO3166 Alpha 2 code, lower cased.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.COUNTRY)
public interface Country extends IdentifiableEntity, AccessibleEntity,
        PermissionScope, ItemHolder {

    public static final String COUNTRY_CODE = Ontology.IDENTIFIER_KEY;

    /**
     * Alias function for fetching the country code identifier.
     *
     * @return The country code
     */
    @Mandatory
    @Property(COUNTRY_CODE)
    public String getCode();

    /**
     * Fetch a count of the number of repositories in this country.
     *
     * @return the repository count
     */
    @Meta(CHILD_COUNT)
    @JavaHandler
    public long getChildCount();

    /**
     * Fetch all repositories in this country.
     *
     * @return an iterable of repository frames
     */
    @Adjacency(label = Ontology.REPOSITORY_HAS_COUNTRY, direction = Direction.IN)
    public Iterable<Repository> getRepositories();

    /**
     * Add a repository to this country.
     *
     * @param repository a repository frame
     */
    @JavaHandler
    public void addRepository(final Repository repository);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Country {

        public long getChildCount() {
            return gremlin().inE(Ontology.REPOSITORY_HAS_COUNTRY).count();
        }

        public void addRepository(final Repository repository) {
            JavaHandlerUtils.addSingleRelationship(repository.asVertex(), it(),
                    Ontology.REPOSITORY_HAS_COUNTRY);
        }
    }
}
