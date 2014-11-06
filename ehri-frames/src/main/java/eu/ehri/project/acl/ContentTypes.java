package eu.ehri.project.acl;

import eu.ehri.project.definitions.Entities;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * An enum of entity types that represent first-class
 * content types, meaning that permissions can be
 * granted up all items of that type. Each value of this
 * enum has an equivalent node in the graph to which
 * individual permission grant nodes refer if they
 * pertain to an entire class of items rather than a
 * single item.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum ContentTypes {
    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT),
    REPOSITORY(Entities.REPOSITORY),
    AUTHORITY(Entities.HISTORICAL_AGENT),
    GROUP(Entities.GROUP),
    USER_PROFILE(Entities.USER_PROFILE),
    ANNOTATION(Entities.ANNOTATION),
    SYSTEM_EVENT(Entities.SYSTEM_EVENT),
    AUTHORITATIVE_SET(Entities.AUTHORITATIVE_SET),
    CVOC_VOCABULARY(Entities.CVOC_VOCABULARY),
    CVOC_CONCEPT(Entities.CVOC_CONCEPT),
    LINK(Entities.LINK),
    COUNTRY(Entities.COUNTRY),
    VIRTUAL_COLLECTION(Entities.VIRTUAL_UNIT);
    
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
