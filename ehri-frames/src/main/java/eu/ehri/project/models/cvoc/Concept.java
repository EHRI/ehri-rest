package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;

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
public interface Concept extends AccessibleEntity, IdentifiableEntity,
        DescribedEntity, AuthoritativeItem, ItemHolder {

    // NB: As an AuthoritativeItem the set will be @Fetched automatically
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public Vocabulary getVocabulary();

    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public void setVocabulary(final Vocabulary vocabulary);

    @Property(CHILD_COUNT)
    public Long getChildCount();

    // relations to other concepts
    
    // Note that multiple broader concepts are possible
    @Fetch(Ontology.CONCEPT_HAS_BROADER)
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction=Direction.IN)
    public Iterable<Concept> getBroaderConcepts();

    // NOTE: don't put a Fetch on it, because it can be a large tree of concepts
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction = Direction.OUT)
    public Iterable<Concept> getNarrowerConcepts();

    @JavaHandler
    public void addNarrowerConcept(final Concept concept);

    @JavaHandler
    public void removeNarrowerConcept(final Concept concept);

    
    // Related concepts, should be like a symmetric associative link... 
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    public Iterable<Concept> getRelatedConcepts();

    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    public void addRelatedConcept(final Concept concept);

    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    public void removeRelatedConcept(final Concept concept);
    
    // Hmm, does not 'feel' symmetric
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED, direction=Direction.IN)
    public Iterable<Concept> getRelatedByConcepts();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl  implements JavaHandlerContext<Vertex>, Concept {

        public Long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().out(Ontology.CONCEPT_HAS_NARROWER).count();
            }
            return count;
        }

        public void addNarrowerConcept(final Concept concept) {
            it().addEdge(Ontology.CONCEPT_HAS_NARROWER, concept.asVertex());
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                it().setProperty(CHILD_COUNT, gremlin().out(Ontology.CONCEPT_HAS_NARROWER).count());
            } else {
                it().setProperty(CHILD_COUNT, count + 1);
            }
        }

        public void removeNarrowerConcept(final Concept concept) {
            for (Edge e : it().getEdges(Direction.OUT, Ontology.CONCEPT_HAS_NARROWER)) {
                if (e.getVertex(Direction.IN).equals(concept.asVertex())) {
                    e.remove();
                    break;
                }
            }
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                getChildCount();
            } else {
                it().setProperty(CHILD_COUNT, count - 1);
            }
        }
    }
}
