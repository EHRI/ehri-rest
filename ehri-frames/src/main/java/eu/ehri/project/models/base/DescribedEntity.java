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

package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

/**
 * An entity that can have descriptions.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public interface DescribedEntity extends PermissionScope, AnnotatableEntity, LinkableEntity {

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public void addDescription(final Description description);

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public void setDescriptions(Iterable<Description> descriptions);

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public void removeDescription(final Description description);

    @Fetch(Ontology.DESCRIPTION_FOR_ENTITY)
    @Dependent
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY, direction = Direction.IN)
    public Iterable<Description> getDescriptions();
}
