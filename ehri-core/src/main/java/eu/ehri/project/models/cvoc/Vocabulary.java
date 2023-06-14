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
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Meta;

/**
 * A collection of 'related' concepts, or maybe a bit like the SKOS Concept Scheme
 * Note that any concept in this Vocabulary that has no parent might be considered a topConcept. 
 */
@EntityType(EntityClass.CVOC_VOCABULARY)
public interface Vocabulary extends AuthoritativeSet {

    @Meta(CHILD_COUNT)
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    Iterable<Concept> getConcepts();

    @JavaHandler
    Iterable<Concept> getTopConcepts();

    abstract class Impl implements JavaHandlerContext<Vertex>, Vocabulary {
        @Override
        public Iterable<Concept> getTopConcepts() {
            return frameVertices(gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET)
                    .filter(v -> !v.getEdges(Direction.IN, Ontology.CONCEPT_HAS_NARROWER).iterator().hasNext()));
        }

    }
}
