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
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.NamedEntity;

/**
 * Holds the information on a relationship specified in some Description,
 * but without the target-end of the relationship being determined.
 * 
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
@EntityType(EntityClass.UNDETERMINED_RELATIONSHIP)
public interface UndeterminedRelationship extends AccessibleEntity, NamedEntity, Annotator {

    /**
     * Fetch the description to which this UR belongs.
     *
     * @return a description frame
     */
    @Adjacency(label = Ontology.HAS_ACCESS_POINT, direction = Direction.IN)
    public Description getDescription();

    /**
     * Fetch the links which make up the body of this UR (if any.)
     *
     * @return an iterable of link frames
     */
    @Adjacency(label = Ontology.LINK_HAS_BODY, direction = Direction.IN)
    public Iterable<Link> getLinks();

    /**
     * Get the relationship type of this UR.
     *
     * @return A type string
     */
    @Mandatory
    @Property(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)
    public String getRelationshipType();
}




