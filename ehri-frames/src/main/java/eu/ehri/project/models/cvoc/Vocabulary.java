package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 * A collection of 'related' concepts, or maybe a bit like the SKOS Concept Scheme
 * Note that any concept in this Vocabulary that has no parent might be considered a topConcept. 
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_VOCABULARY)
public interface Vocabulary extends AccessibleEntity, PermissionScope {
    public static final String LABEL = "cvocLabel";
    public static final String IN_CVOC = "inCvoc";

    @Adjacency(label = IN_CVOC, direction = Direction.IN)
    public Iterable<Concept> getConcepts();

    @Adjacency(label = IN_CVOC, direction = Direction.IN)
    public void addConcept(final Concept concept);

    // textual information
    
    @Fetch
    @Dependent
    @Adjacency(label = LABEL)
    public Iterable<Text> getLabel();

    // NOTE: we should only allow one label per language, but we cannot model that
    @Adjacency(label = LABEL)
    public void addLabel(final Text label);
   
    @Adjacency(label = LABEL)
    public void removeLabel(final Text label);

}
