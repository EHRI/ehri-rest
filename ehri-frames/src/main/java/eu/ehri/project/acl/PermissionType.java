package eu.ehri.project.acl;

public enum PermissionType {
    CREATE("create", 0x1),
    UPDATE("update", 0x2),
    DELETE("delete", 0x4),
    GRANT("grant", 0x8),
    ANNOTATE("annotate", 0x16),
    OWNER("owner", 0x23);
    
    private final String name;
    private final int mask;
    
    private PermissionType(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    public String getName() {
        return name;
    }

    public int getMask() {
        return mask;
    }
    
    @Override public String toString() {
        return name;
    }
    
    public static PermissionType withName(String name) {
        for (PermissionType p : PermissionType.values()) {
            if (p.getName().equals(name))
                return p;
        }
        throw new IllegalArgumentException("Invalid permission type: " + name);
    }    
}
