package eu.ehri.project.models;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.SystemEventQueue;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;

/**
 * Mapping of Entity type names to Frames interfaces classes and
 * their associated IdGenerator functors.
 * 
 * @author michaelb
 *
 */
public enum EntityClass {

    // @formatter:off
    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT, "c", DocumentaryUnit.class, IdentifiableEntityIdGenerator.INSTANCE),
    REPOSITORY(Entities.REPOSITORY, "r", Repository.class, IdentifiableEntityIdGenerator.INSTANCE),
    HISTORICAL_AGENT(Entities.HISTORICAL_AGENT, "a", HistoricalAgent.class, IdentifiableEntityIdGenerator.INSTANCE),
    GROUP(Entities.GROUP, "g", Group.class, IdentifiableEntityIdGenerator.INSTANCE),
    USER_PROFILE(Entities.USER_PROFILE, "u", UserProfile.class, IdentifiableEntityIdGenerator.INSTANCE),
    AUTHORITATIVE_SET(Entities.AUTHORITATIVE_SET, "as", AuthoritativeSet.class, IdentifiableEntityIdGenerator.INSTANCE),
    
    // Generic entities.
    DOCUMENT_DESCRIPTION(Entities.DOCUMENT_DESCRIPTION, "dd", DocumentDescription.class),
    REPOSITORY_DESCRIPTION(Entities.REPOSITORY_DESCRIPTION, "rd", RepositoryDescription.class),
    HISTORICAL_AGENT_DESCRIPTION(Entities.HISTORICAL_AGENT_DESCRIPTION, "ad", HistoricalAgentDescription.class),
    DATE_PERIOD(Entities.DATE_PERIOD, "dp", DatePeriod.class),
    ANNOTATION(Entities.ANNOTATION, "ann", Annotation.class),
    ADDRESS(Entities.ADDRESS, "adr", Address.class),
    SYSTEM_EVENT(Entities.SYSTEM_EVENT, "ev", SystemEvent.class, GenericIdGenerator.INSTANCE),
    SYSTEM(Entities.SYSTEM, "sys", SystemEventQueue.class),
    PROPERTY(Entities.PROPERTY, "p", Property.class),
    PERMISSION(Entities.PERMISSION, "pm", Permission.class),
    PERMISSION_GRANT(Entities.PERMISSION_GRANT, "pmg", PermissionGrant.class),
    CONTENT_TYPE(Entities.CONTENT_TYPE, "ct", ContentType.class),
    CVOC_VOCABULARY(Entities.CVOC_VOCABULARY, "cvv", Vocabulary.class),
    CVOC_CONCEPT(Entities.CVOC_CONCEPT, "cv", Concept.class),
    CVOC_CONCEPT_DESCRIPTION(Entities.CVOC_CONCEPT_DESCRIPTION, "cvd", ConceptDescription.class),
    MAINTENANCE_EVENT ( Entities.MAINTENANCE_EVENT, "me", MaintenanceEvent.class),
    UNDETERMINED_RELATIONSHIP (Entities.UNDETERMINED_RELATIONSHIP, "rs", UndeterminedRelationship.class);
    // @formatter:on

    // Accessors.

    /**
     * Get the string name of this EntityType.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get the abbreviation code of this entityType.
     * 
     * @return
     */
    public String getAbbreviation() {
        return abbr;
    }

    /**
     * Get the interface to which this EntityType refers.
     * 
     * @return
     */
    public Class<? extends Frame> getEntityClass() {
        return cls;
    }

    /**
     * Get the ID generator class for this entityType.
     * 
     * @return
     */
    public IdGenerator getIdgen() {
        return idgen;
    }

    public static EntityClass withName(String name) {
        for (EntityClass et : EntityClass.values()) {
            if (et.getName().equals(name))
                return et;
        }
        throw new IllegalArgumentException("Invalid entity type: " + name);
    }

    private final String name;
    private final String abbr;
    private final Class<? extends Frame> cls;
    private final IdGenerator idgen;

    private EntityClass(String name, String abbr,
            Class<? extends Frame> cls, IdGenerator idgen) {
        this.name = name;
        this.abbr = abbr;
        this.cls = cls;
        this.idgen = idgen;
    }

    /**
     * Short constructor.
     * 
     * @param name
     * @param abbr
     * @param cls
     */
    private EntityClass(String name, String abbr,
            Class<? extends Frame> cls) {
        this(name, abbr, cls, GenericIdGenerator.INSTANCE);
    }

    @Override
    public String toString() {
        return name;
    }
}
