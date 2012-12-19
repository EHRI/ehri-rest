package eu.ehri.project.acl;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

import eu.ehri.project.definitions.Entities;

public enum ContentTypes {
    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT),
    AGENT(Entities.AGENT),
    AUTHORITY(Entities.AUTHORITY),
    GROUP(Entities.GROUP),
    USER_PROFILE(Entities.USER_PROFILE),
    ANNOTATION(Entities.ANNOTATION),
    ACTION(Entities.ACTION);
    
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
        throw new IllegalArgumentException("Invalid content type type: " + name);
    }    

}
