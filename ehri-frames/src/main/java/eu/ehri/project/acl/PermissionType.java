package eu.ehri.project.acl;

public enum PermissionType {
    CREATE("create", 1),
    UPDATE("update", 2),
    DELETE("delete", 4),
    ANNOTATE("annotate", 8),
    OWNER("owner", 15), // Implies C,U,D,A
    GRANT("grant", 16);
    
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
