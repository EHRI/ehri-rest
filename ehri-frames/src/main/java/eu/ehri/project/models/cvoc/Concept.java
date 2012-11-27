package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * This models the thesaurus terms or keywords in a way that is better managing multi-linguality. 
 * The terms are then labels of concepts in a specific language and concepts can have many labels.  
 * This is following the SKOS-Core concept, but maybe not fully so we don't use 'SKOS' in the class name. 
 * For SKOS core see: http://www.w3.org/2009/08/skos-reference/skos.html
 * 
 * A nice glossary of terms used with controlled vocabularies can be found here: 
 * http://www.willpowerinfo.co.uk/glossary.htm
 * 
 * @author paulboon
 *
 */
@EntityType(EntityTypes.CVOC_CONCEPT)
public interface Concept extends AccessibleEntity {
    //public static final String BROADER = "broader";
    public static final String NARROWER = "narrower";
    public static final String RELATED = "related";
    public static final String PREFLABEL = "prefLabel";
    public static final String ALTLABEL = "altLabel";
    public static final String DEFINITION = "definition";
    public static final String SCOPENOTE = "scopeNote";

    // relations to other concepts
    
    // Note that multiple broader concepts are possible
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

    
    // Related concepts, should be symetric associative link... 
    @Adjacency(label = RELATED)
    public Iterable<Concept> getRelatedConcepts();

    @Adjacency(label = RELATED)
    public void addRelatedConcept(final Concept concept);

    @Adjacency(label = RELATED)
    public void removeRelatedConcept(final Concept concept);
    
    // Hmm, does not 'feel' symmetric
    @Adjacency(label = RELATED, direction=Direction.IN)
    public Iterable<Concept> getRelatedByConcepts();
    
    // textual information
    
    @Fetch
    @Dependent
    @Adjacency(label = PREFLABEL)
    public Iterable<Text> getPrefLabel();

    // NOTE: we should only allow one prefLabel per language, but we cannot model that
    @Adjacency(label = PREFLABEL)
    public void addPrefLabel(final Text prefLabel);
   
    @Adjacency(label = PREFLABEL)
    public void removePrefLabel(final Text prefLabel);

    @Fetch
    @Dependent
    @Adjacency(label = ALTLABEL)
    public Iterable<Text> getAltLabels();

    @Adjacency(label = ALTLABEL)
    public void addAltLabel(final Text altLabel);

    @Adjacency(label = ALTLABEL)
    public void removeAltLabel(final Text altLabel);

    @Fetch
    @Dependent
    @Adjacency(label = DEFINITION)
    public Iterable<Text> getDefinitions();

    @Adjacency(label = DEFINITION)
    public void addDefinition(final Text definition);
    
    @Adjacency(label = DEFINITION)
    public void removeDefinition(final Text definition);

    // NOTE: why has SKOS definitions and scope notes, what is the difference?
    // scope notes seem to be used in a more flexible way 
    // and can contain some extra information besides describing the definition. 
    @Fetch
    @Dependent
    @Adjacency(label = SCOPENOTE)
    public Iterable<Text> getScopeNotes();

    @Adjacency(label = SCOPENOTE)
    public void addScopeNote(final Text scopeNote);
    
    @Adjacency(label = SCOPENOTE)
    public void removeScopeNote(final Text scopeNote);
}
