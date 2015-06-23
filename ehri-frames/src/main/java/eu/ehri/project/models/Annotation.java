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

package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.Promotable;

/**
 * A frame class representing an annotation.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.ANNOTATION)
public interface Annotation extends Promotable {

    /**
     * Fetch annotations that have been made <i>on this annotation.</i>
     *
     * @return an iterable of annotation frames
     */
    @Fetch(Ontology.ANNOTATION_ANNOTATES)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES, direction = Direction.IN)
    Iterable<Annotation> getAnnotations();

    /**
     * Fetch the annotator of this annotation.
     *
     * @return an annotator frame
     */
    @Fetch(value = Ontology.ANNOTATOR_HAS_ANNOTATION, numLevels = 0)
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION, direction = Direction.IN)
    Annotator getAnnotator();

    /**
     * Set the annotator of this annotation.
     *
     * @param annotator an annotator frame
     */
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION, direction = Direction.IN)
    void setAnnotator(Annotator annotator);

    /**
     * Fetch all targets of this annotation, including sub-parts
     * of an item.
     *
     * @return an iterable of annotatable items
     */
    @Fetch(value = Ontology.ANNOTATES_PART, numLevels = 1)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES_PART)
    Iterable<AnnotatableEntity> getTargetParts();

    /**
     * Fetch the targets of this annotation.
     *
     * @return an iterable of annotatable items
     */
    @Fetch(value = Ontology.ANNOTATES, numLevels = 1)
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES)
    Iterable<AnnotatableEntity> getTargets();

    /**
     * Get the body of this annotation.
     *
     * @return the body text
     */
    @Mandatory
    @Property(Ontology.ANNOTATION_NOTES_BODY)
    String getBody();
}
