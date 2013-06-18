package eu.ehri.project.exceptions;

public class AccessDenied extends Exception {

    private static final long serialVersionUID = -7496196761160357738L;
    private String accessor = null;
    private String entity = null;

    public AccessDenied(String accessor, String entity) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s'",
                entity, accessor));
        this.accessor = accessor;
        this.entity = entity;
    }

    public String getAccessor() {
        return accessor;
    }

    public String getEntity() {
        return entity;
    }
}
