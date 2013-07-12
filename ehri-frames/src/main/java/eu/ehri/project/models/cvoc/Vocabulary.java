package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 * A collection of 'related' concepts, or maybe a bit like the SKOS Concept Scheme
 * Note that any concept in this Vocabulary that has no parent might be considered a topConcept. 
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_VOCABULARY)
public interface Vocabulary extends AccessibleEntity, IdentifiableEntity, PermissionScope, AuthoritativeSet {
    @Adjacency(label = IN_SET, direction = Direction.IN)
    public Iterable<Concept> getConcepts();

    @Adjacency(label = IN_SET, direction = Direction.IN)
    public void addConcept(final Concept concept);
}
