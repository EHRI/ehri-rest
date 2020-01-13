/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import eu.ehri.project.models.Link;

/**
 * An entity that can hold incoming links.
 */
public interface Linkable extends Accessible {
    /**
     * Fetch links attached to this item.
     *
     * @return an iterable of link frames
     */
    @Adjacency(label = Ontology.LINK_HAS_TARGET, direction = Direction.IN)
    Iterable<Link> getLinks();

    /**
     * Add a link to this item.
     *
     * @param link a link frame
     */
    @Adjacency(label = Ontology.LINK_HAS_TARGET, direction = Direction.IN)
    void addLink(Link link);
}
