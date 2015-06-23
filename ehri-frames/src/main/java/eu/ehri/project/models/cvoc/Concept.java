/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
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
import eu.ehri.project.models.utils.JavaHandlerUtils;

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
 * @author Paul Boon (http://github.com/PaulBoon)
 */
@EntityType(EntityClass.CVOC_CONCEPT)
public interface Concept extends
        DescribedEntity, AuthoritativeItem, ItemHolder {

    // NB: As an AuthoritativeItem the set will be @Fetched automatically
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    Vocabulary getVocabulary();

    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    void setVocabulary(Vocabulary vocabulary);

    @Property(CHILD_COUNT)
    long getChildCount();

    // relations to other concepts
    
    // Note that multiple broader concepts are possible
    @Fetch(Ontology.CONCEPT_HAS_BROADER)
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction=Direction.IN)
    Iterable<Concept> getBroaderConcepts();

    // NOTE: don't put a Fetch on it, because it can be a large tree of concepts
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction = Direction.OUT)
    Iterable<Concept> getNarrowerConcepts();

    @JavaHandler
    void addNarrowerConcept(Concept concept);

    @JavaHandler
    void removeNarrowerConcept(Concept concept);

    
    // Related concepts, should be like a symmetric associative link... 
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    Iterable<Concept> getRelatedConcepts();

    @JavaHandler
    void addRelatedConcept(Concept concept);

    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    void removeRelatedConcept(Concept concept);
    
    // Hmm, does not 'feel' symmetric
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED, direction=Direction.IN)
    Iterable<Concept> getRelatedByConcepts();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl  implements JavaHandlerContext<Vertex>, Concept {

        public long getChildCount() {
            return gremlin().outE(Ontology.CONCEPT_HAS_NARROWER).count();
        }

        public void addRelatedConcept(Concept related) {
            JavaHandlerUtils.addUniqueRelationship(it(),
                    related.asVertex(), Ontology.CONCEPT_HAS_RELATED);
        }

        public void addNarrowerConcept(Concept concept) {
            if (!concept.asVertex().equals(it())) {
                JavaHandlerUtils.addUniqueRelationship(it(),
                        concept.asVertex(), Ontology.CONCEPT_HAS_NARROWER);
            }
        }

        public void removeNarrowerConcept(Concept concept) {
            JavaHandlerUtils.removeAllRelationships(it(), concept.asVertex(),
                    Ontology.CONCEPT_HAS_NARROWER);
        }
    }
}
