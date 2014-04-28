package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * A collection of 'related' concepts, or maybe a bit like the SKOS Concept Scheme
 * Note that any concept in this Vocabulary that has no parent might be considered a topConcept. 
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_VOCABULARY)
public interface Vocabulary extends AccessibleEntity, IdentifiableEntity,
        PermissionScope, AuthoritativeSet, ItemHolder {

    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    public Iterable<Concept> getConcepts();

    @JavaHandler
    public void addConcept(final Concept concept);

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Vocabulary {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET).count());
        }

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET).count();
            }
            return count;
        }

        public void addConcept(final Concept concept) {
            if (JavaHandlerUtils.addSingleRelationship(concept.asVertex(), it(),
                    Ontology.ITEM_IN_AUTHORITATIVE_SET)) {
                updateChildCountCache();
            }
        }
    }
}
