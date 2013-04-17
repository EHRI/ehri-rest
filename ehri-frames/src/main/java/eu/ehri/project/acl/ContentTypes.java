package eu.ehri.project.acl;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

import eu.ehri.project.definitions.Entities;

public enum ContentTypes {
    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT),
    AGENT(Entities.REPOSITORY),
    AUTHORITY(Entities.HISTORICAL_AGENT),
    GROUP(Entities.GROUP),
    USER_PROFILE(Entities.USER_PROFILE),
    ANNOTATION(Entities.ANNOTATION),
    SYSTEM_EVENT(Entities.SYSTEM_EVENT),
    AUTHORITATIVE_SET(Entities.AUTHORITATIVE_SET),
    CVOC_VOCABULARY(Entities.CVOC_VOCABULARY),
    CVOC_CONCEPT(Entities.CVOC_CONCEPT);
    
    private final String name;
    
    private ContentTypes(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override public String toString() {
        return name;
    }
    
    @JsonCreator
    public static ContentTypes withName(String name) {
        for (ContentTypes c : ContentTypes.values()) {
            if (c.getName().equals(name))
                return c;
        }
        throw new IllegalArgumentException("Invalid content type type: '" + name + "'");
    }
}
