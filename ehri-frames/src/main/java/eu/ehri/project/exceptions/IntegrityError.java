package eu.ehri.project.exceptions;

public class IntegrityError extends Exception {
    private String idValue;
    public IntegrityError(String idValue) {
        super("Integrity error for id value: " + idValue);
        this.idValue = idValue;
    }

    public String getIdValue() {
        return idValue;
    }
    private static final long serialVersionUID = -5625119780401587251L;

}
