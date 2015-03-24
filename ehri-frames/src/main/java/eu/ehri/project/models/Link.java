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
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.LinkableEntity;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Links two items together with a given body, with may either be
 * a text property or some other entity.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.LINK)
public interface Link extends AccessibleEntity, AnnotatableEntity, Promotable, TemporalEntity {

    @Fetch(value = Ontology.LINK_HAS_LINKER, numLevels = 0)
    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    public UserProfile getLinker();

    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    public void setLinker(final Accessor accessor);

    @Fetch(value = Ontology.LINK_HAS_TARGET, ifLevel = 0, numLevels = 1)
    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    public Iterable<LinkableEntity> getLinkTargets();

    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    public void addLinkTarget(final LinkableEntity entity);

    @Fetch(Ontology.LINK_HAS_BODY)
    @Adjacency(label = Ontology.LINK_HAS_BODY)
    public Iterable<AccessibleEntity> getLinkBodies();

    @Adjacency(label = Ontology.LINK_HAS_BODY)
    public void addLinkBody(final AccessibleEntity entity);

    @Mandatory
    @Property(Ontology.LINK_HAS_TYPE)
    public String getLinkType();

    @Property(Ontology.LINK_HAS_DESCRIPTION)
    public String getDescription();
}




