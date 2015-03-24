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
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Frame class representing a date period.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.DATE_PERIOD)
public interface DatePeriod extends AnnotatableEntity {

    /**
     * The start date in UTC format.
     *
     * @return A UTC date string
     */
    @Property(Ontology.DATE_PERIOD_START_DATE)
    public String getStartDate();

    /**
     * The end date in UTC format.
     *
     * @return A UTC string
     */
    @Property(Ontology.DATE_PERIOD_END_DATE)
    public String getEndDate();

    /**
     * Get the entity described by this date period.
     *
     * @return a temporal item
     */
    @Adjacency(label = Ontology.ENTITY_HAS_DATE, direction = Direction.IN)
    public TemporalEntity getEntity();
}
