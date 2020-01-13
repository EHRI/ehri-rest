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

package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.*;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.models.base.Temporal;

/**
 * Links two items together with a given body, with may either be
 * a text property or some other entity.
 */
@EntityType(EntityClass.LINK)
public interface Link extends Promotable, Temporal, Annotatable {

    @Fetch(value = Ontology.LINK_HAS_LINKER, numLevels = 0)
    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    UserProfile getLinker();

    @UniqueAdjacency(label = Ontology.LINK_HAS_LINKER)
    void setLinker(Accessor accessor);

    /**
     * Fetch the source of this link, or null if the link
     * is not directional.
     *
     * @return a linkable item, or null
     */
    @Fetch(value = Ontology.LINK_HAS_SOURCE, ifLevel = 0, numLevels = 1)
    @Adjacency(label = Ontology.LINK_HAS_SOURCE)
    Linkable getLinkSource();

    /**
     * Set the source of this link if it is directional.
     *
     * @param entity a linkable item
     */
    @UniqueAdjacency(label = Ontology.LINK_HAS_SOURCE)
    void setLinkSource(Linkable entity);

    /**
     * Fetch the targets attached to this link.
     *
     * @return an iterable of linkable items
     */
    @Fetch(value = Ontology.LINK_HAS_TARGET, ifLevel = 0, numLevels = 1)
    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    Iterable<Linkable> getLinkTargets();

    /**
     * Add a target to this link.
     *
     * @param entity a linkable item
     */
    @UniqueAdjacency(label = Ontology.LINK_HAS_TARGET)
    void addLinkTarget(Linkable entity);

    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    void removeLinkTarget(Linkable entity);

    @Fetch(Ontology.LINK_HAS_BODY)
    @Adjacency(label = Ontology.LINK_HAS_BODY)
    Iterable<Accessible> getLinkBodies();

    @UniqueAdjacency(label = Ontology.LINK_HAS_BODY)
    void addLinkBody(Accessible entity);

    @Mandatory
    @Property(Ontology.LINK_HAS_TYPE)
    String getLinkType();

    @Indexed
    @Property(Ontology.LINK_HAS_FIELD)
    String getLinkField();

    @Property(Ontology.LINK_HAS_DESCRIPTION)
    String getDescription();
}




