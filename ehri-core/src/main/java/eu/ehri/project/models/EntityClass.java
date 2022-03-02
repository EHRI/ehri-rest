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

package eu.ehri.project.models;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.EventLink;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.SystemEventQueue;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.models.idgen.DescriptionIdGenerator;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;

/**
 * Mapping of Entity type names to Frames interfaces classes and
 * their associated id generator classes.
 */
public enum EntityClass {

    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT, DocumentaryUnit.class, IdentifiableEntityIdGenerator.INSTANCE),
    REPOSITORY(Entities.REPOSITORY, Repository.class, IdentifiableEntityIdGenerator.INSTANCE),
    HISTORICAL_AGENT(Entities.HISTORICAL_AGENT, HistoricalAgent.class, IdentifiableEntityIdGenerator.INSTANCE),
    GROUP(Entities.GROUP, Group.class, IdentifiableEntityIdGenerator.INSTANCE),
    USER_PROFILE(Entities.USER_PROFILE, UserProfile.class, IdentifiableEntityIdGenerator.INSTANCE),
    AUTHORITATIVE_SET(Entities.AUTHORITATIVE_SET, AuthoritativeSet.class, IdentifiableEntityIdGenerator.INSTANCE),
    COUNTRY(Entities.COUNTRY, Country.class, IdentifiableEntityIdGenerator.INSTANCE),
    CVOC_VOCABULARY(Entities.CVOC_VOCABULARY, Vocabulary.class, IdentifiableEntityIdGenerator.INSTANCE),
    CVOC_CONCEPT(Entities.CVOC_CONCEPT, Concept.class, IdentifiableEntityIdGenerator.INSTANCE),
    VIRTUAL_UNIT(Entities.VIRTUAL_UNIT, VirtualUnit.class, IdentifiableEntityIdGenerator.INSTANCE),

    DOCUMENTARY_UNIT_DESCRIPTION(Entities.DOCUMENTARY_UNIT_DESCRIPTION, DocumentaryUnitDescription.class, DescriptionIdGenerator.INSTANCE),
    REPOSITORY_DESCRIPTION(Entities.REPOSITORY_DESCRIPTION, RepositoryDescription.class, DescriptionIdGenerator.INSTANCE),
    HISTORICAL_AGENT_DESCRIPTION(Entities.HISTORICAL_AGENT_DESCRIPTION, HistoricalAgentDescription.class, DescriptionIdGenerator.INSTANCE),
    CVOC_CONCEPT_DESCRIPTION(Entities.CVOC_CONCEPT_DESCRIPTION, ConceptDescription.class, DescriptionIdGenerator.INSTANCE),

    // Generic entities.
    DATE_PERIOD(Entities.DATE_PERIOD, DatePeriod.class),
    ANNOTATION(Entities.ANNOTATION, Annotation.class),
    ADDRESS(Entities.ADDRESS, Address.class),
    SYSTEM_EVENT(Entities.SYSTEM_EVENT, SystemEvent.class),
    VERSION(Entities.VERSION, Version.class),
    SYSTEM(Entities.SYSTEM, SystemEventQueue.class),
    UNKNOWN_PROPERTY(Entities.UNKNOWN_PROPERTY, UnknownProperty.class),
    PERMISSION(Entities.PERMISSION, Permission.class),
    PERMISSION_GRANT(Entities.PERMISSION_GRANT, PermissionGrant.class),
    CONTENT_TYPE(Entities.CONTENT_TYPE, ContentType.class),
    MAINTENANCE_EVENT(Entities.MAINTENANCE_EVENT, MaintenanceEvent.class),
    ACCESS_POINT(Entities.ACCESS_POINT, AccessPoint.class),
    LINK(Entities.LINK, Link.class),
    EVENT_LINK(Entities.EVENT_LINK, EventLink.class);

    // Accessors.

    /**
     * Get the string name of this EntityType.
     *
     * @return the type's name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the interface to which this EntityType refers.
     *
     * @return the Java model class associated with the type
     */
    public Class<? extends Entity> getJavaClass() {
        return cls;
    }

    /**
     * Get the ID generator class for this entityType.
     *
     * @return the IdGenerator instance associated with the type
     */
    public IdGenerator getIdGen() {
        return idGen;
    }

    public static EntityClass withName(String name) {
        for (EntityClass et : EntityClass.values()) {
            if (et.getName().equals(name))
                return et;
        }
        throw new IllegalArgumentException("Invalid entity type: " + name);
    }

    private final String name;
    private final Class<? extends Entity> cls;
    private final IdGenerator idGen;

    EntityClass(String name, Class<? extends Entity> cls, IdGenerator idGen) {
        this.name = name;
        this.cls = cls;
        this.idGen = idGen;
    }

    EntityClass(String name, Class<? extends Entity> cls) {
        this(name, cls, GenericIdGenerator.INSTANCE);
    }

    @Override
    public String toString() {
        return name;
    }
}
