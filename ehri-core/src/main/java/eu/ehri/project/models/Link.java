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

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
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

    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    void setLinker(Accessor accessor);

    @Fetch(value = Ontology.LINK_HAS_TARGET, ifLevel = 0, numLevels = 1)
    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    Iterable<Linkable> getLinkTargets();

    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    void addLinkTarget(Linkable entity);

    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    void removeLinkTarget(Linkable entity);

    @Fetch(Ontology.LINK_HAS_BODY)
    @Adjacency(label = Ontology.LINK_HAS_BODY)
    Iterable<Accessible> getLinkBodies();

    @Adjacency(label = Ontology.LINK_HAS_BODY)
    void addLinkBody(Accessible entity);

    @Mandatory
    @Property(Ontology.LINK_HAS_TYPE)
    String getLinkType();

    @Property(Ontology.LINK_HAS_FIELD)
    String getLinkField();

    @Property(Ontology.LINK_HAS_DESCRIPTION)
    String getDescription();
}




