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
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Indexed;
import eu.ehri.project.models.annotations.Mandatory;

/**
 * An entity that describes another entity.
 *
 */
public interface Description extends Named, Accessible {

    /**
     * Process by which this description was created. Currently supported
     * values allow for automatic import or manual creation (by a human).
     */
    enum CreationProcess {
        MANUAL, IMPORT
    }

    @Mandatory
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY)
    Described getEntity();

    @Mandatory
    @Property(Ontology.LANGUAGE_OF_DESCRIPTION)
    String getLanguageOfDescription();

    @Indexed
    @Property(Ontology.IDENTIFIER_KEY)
    String getDescriptionCode();

    @Indexed
    @Property(Ontology.CREATION_PROCESS)
    CreationProcess getCreationProcess();

    /**
     * Get the described entity of a description. This 
     * method if @Fetch serialized only if the description
     * is at the top level of the requested subtree.
     */
    @Fetch(value = Ontology.DESCRIPTION_FOR_ENTITY, ifLevel =0)
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY)
    Described getDescribedEntity();

    @Dependent
    @Fetch(value = Ontology.HAS_MAINTENANCE_EVENT, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    Iterable<MaintenanceEvent> getMaintenanceEvents();

    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    void setMaintenanceEvents(Iterable<MaintenanceEvent> maintenanceEvents);

    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    void addMaintenanceEvent(MaintenanceEvent maintenanceEvent);

    @Dependent
    @Fetch(value = Ontology.HAS_ACCESS_POINT, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_ACCESS_POINT)
    Iterable<AccessPoint> getAccessPoints();

    @Adjacency(label = Ontology.HAS_ACCESS_POINT)
    void addAccessPoint(AccessPoint accessPoint);

    @Dependent
    @Fetch(value = Ontology.HAS_UNKNOWN_PROPERTY, ifLevel = 1, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_UNKNOWN_PROPERTY)
    Iterable<UnknownProperty> getUnknownProperties();
}
