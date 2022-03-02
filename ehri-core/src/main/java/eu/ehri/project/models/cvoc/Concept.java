/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.*;
import eu.ehri.project.models.base.Described;
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
 */
@EntityType(EntityClass.CVOC_CONCEPT)
public interface Concept extends Described, AuthoritativeItem, ItemHolder {

    // NB: As an AuthoritativeItem the set will be @Fetched automatically
    @Mandatory
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    Vocabulary getVocabulary();

    @UniqueAdjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, single = true)
    void setVocabulary(Vocabulary vocabulary);

    @Meta(CHILD_COUNT)
    @UniqueAdjacency(label = Ontology.CONCEPT_HAS_NARROWER)
    int countChildren();

    // relations to other concepts
    
    // Note that multiple broader concepts are possible
    @Fetch(Ontology.CONCEPT_HAS_BROADER)
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction=Direction.IN)
    Iterable<Concept> getBroaderConcepts();

    // NOTE: don't put a Fetch on it, because it can be a large tree of concepts
    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction = Direction.OUT)
    @InverseOf(Ontology.CONCEPT_HAS_BROADER)
    Iterable<Concept> getNarrowerConcepts();

    @UniqueAdjacency(label = Ontology.CONCEPT_HAS_NARROWER)
    void addNarrowerConcept(Concept concept);

    @UniqueAdjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction = Direction.IN)
    void addBroaderConcept(Concept concept);

    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER)
    void removeNarrowerConcept(Concept concept);

    @Adjacency(label = Ontology.CONCEPT_HAS_NARROWER, direction = Direction.IN)
    void removeBroaderConcept(Concept concept);

    // Related concepts, should be like a symmetric associative link...
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    Iterable<Concept> getRelatedConcepts();

    @UniqueAdjacency(label = Ontology.CONCEPT_HAS_RELATED)
    void addRelatedConcept(Concept concept);

    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED)
    void removeRelatedConcept(Concept concept);
    
    // Hmm, does not 'feel' symmetric
    @Adjacency(label = Ontology.CONCEPT_HAS_RELATED, direction=Direction.IN)
    Iterable<Concept> getRelatedByConcepts();
}
