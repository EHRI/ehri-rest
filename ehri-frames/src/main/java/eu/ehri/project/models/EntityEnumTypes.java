package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.DocumentaryUnitIdGenerator;
import eu.ehri.project.models.idgen.GenericIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;

public enum EntityEnumTypes {

    // @formatter:off
    DOCUMENTARY_UNIT("documentaryUnit", "c", DocumentaryUnit.class, DocumentaryUnitIdGenerator.class),
    AGENT("agent", "r", Agent.class, AccessibleEntityIdGenerator.class),
    AUTHORITY("authority", "a", Authority.class, AccessibleEntityIdGenerator.class),
    GROUP("group", "g", Group.class, AccessibleEntityIdGenerator.class),
    USER_PROFILE("userProfile", "u", UserProfile.class, AccessibleEntityIdGenerator.class),
    
    // Generic entities.
    DOCUMENT_DESCRIPTION("documentDescription", "dd", DocumentDescription.class),
    AGENT_DESCRIPTION("agentDescription", "rd", AgentDescription.class),
    AUTHORITY_DESCRIPTION("authorityDescription", "ad", AuthorityDescription.class),
    DATE_PERIOD("datePeriod", "dp", DatePeriod.class),
    ANNOTATION("annotation", "ann", Annotation.class),
    ADDRESS("address", "adr", Address.class),
    ACTION("action", "act", Action.class),
    IMPORT("import", "imp", Import.class),
    PROPERTY("property", "p", Property.class),
    PERMISSION("permission", "pm", Permission.class),
    PERMISSION_GRANT("permissionGrant", "pmg", PermissionGrant.class),
    CONTENT_TYPE("contentType", "ct", ContentType.class),
    REVISION("revision", "rv", Revision.class);
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
    public Class<? extends VertexFrame> getEntityClass() {
        return cls;
    }

    /**
     * Get the ID generator class for this entityType.
     * 
     * @return
     */
    public Class<? extends IdGenerator> getIdgen() {
        return idgen;
    }
    
    public static EntityEnumTypes withName(String name) {
        for (EntityEnumTypes et : EntityEnumTypes.values()) {
            if (et.getName().equals(name))
                return et;
        }
        throw new IllegalArgumentException("Invalid entity type: " + name);
    }

    private final String name;
    private final String abbr;
    private final Class<? extends VertexFrame> cls;
    private final Class<? extends IdGenerator> idgen;

    private EntityEnumTypes(String name, String abbr,
            Class<? extends VertexFrame> cls, Class<? extends IdGenerator> idgen) {
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
    private EntityEnumTypes(String name, String abbr,
            Class<? extends VertexFrame> cls) {
        this(name, abbr, cls, GenericIdGenerator.class);
    }
    
    @Override public String toString() {
        return name;
    }
}
