package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;

/**
 * This models the thesaurus terms or keywords in a way that is better managing multi-linguality. 
 * The terms are then labels of concepts in a specific language and concepts can have many labels.  
 * This is following the SKOS-Core concept, but not fully so we don't use 'SKOS' in the class name. 
 * For SKOS core see: http://www.w3.org/2009/08/skos-reference/skos.html
 * 
 * Also note that the labels and textual information of a single language 
 * are all placed in a ConceptDescription, following the pattern 
 * that the textual details of an entity are described by a Description entity.  
 * 
 * A nice glossary of terms used with controlled vocabularies can be found here: 
 * http://www.willpowerinfo.co.uk/glossary.htm
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_CONCEPT)
//public interface Concept extends AccessibleEntity {
public interface Concept extends AccessibleEntity, DescribedEntity {
    //public static final String BROADER = "broader";
    public static final String NARROWER = "narrower";
    public static final String RELATED = "related";
    
    @Fetch(Vocabulary.IN_CVOC)
    @Adjacency(label = Vocabulary.IN_CVOC)
    public Vocabulary getVocabulary();

    @Adjacency(label = Vocabulary.IN_CVOC)
    public void setVocabulary(final Vocabulary vocabulary);


    // relations to other concepts
    
    // Note that multiple broader concepts are possible
    @Fetch(NARROWER)
    @Adjacency(label = NARROWER, direction=Direction.IN)
    //@Adjacency(label = BROADER)
    public Iterable<Concept> getBroaderConcepts();

    //@Adjacency(label = BROADER)
    //public void addBroaderConcept(final Concept concept);

    // NOTE: don't put a Fetch on it, because it can be a large tree of concepts
    @Adjacency(label = NARROWER)
    public Iterable<Concept> getNarrowerConcepts();

    @Adjacency(label = NARROWER)
    public void addNarrowerConcept(final Concept concept);

    @Adjacency(label = NARROWER)
    public void removeNarrowerConcept(final Concept concept);

    
    // Related concepts, should be like a symmetric associative link... 
    @Adjacency(label = RELATED)
    public Iterable<Concept> getRelatedConcepts();

    @Adjacency(label = RELATED)
    public void addRelatedConcept(final Concept concept);

    @Adjacency(label = RELATED)
    public void removeRelatedConcept(final Concept concept);
    
    // Hmm, does not 'feel' symmetric
    @Adjacency(label = RELATED, direction=Direction.IN)
    public Iterable<Concept> getRelatedByConcepts();
    
}
