package eu.ehri.project.acl;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

public enum PermissionType {
    CREATE("create", 1)
    , UPDATE("update", 2)
    , DELETE("delete", 4)
    , ANNOTATE("annotate", 8)
    , OWNER("owner", 15) // Implies C,U,D,A
    , GRANT("grant", 16)

    , PROMOTE("promote", 32)
    // Reserved permission types
//    RESERVED2("reserved2", 32),
//    RESERVED3("reserved3", 64),
//    RESERVED4("reserved4", 128)
    ;

    
    private final String name;
    private final int mask;
    
    private PermissionType(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override public String toString() {
        return name;
    }

    /**
     * Return whether a given other permission is encompassed
     * by the current one...
     * @param other
     * @return
     */
    public boolean contains(PermissionType other) {
        return (mask & other.mask) == other.mask;
    }
    
    @JsonCreator
    public static PermissionType withName(String name) {
        for (PermissionType p : PermissionType.values()) {
            if (p.getName().equals(name))
                return p;
        }
        throw new IllegalArgumentException("Invalid permission type: " + name);
    }    
}
