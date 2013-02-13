package eu.ehri.project.exceptions;

public class IntegrityError extends Exception {
    private String idValue;
    public IntegrityError(String idValue) {
        super(String.format("Integity error for id value: %s", idValue));
        this.idValue = idValue;
    }

    public String getIdValue() {
        return idValue;
    }
    private static final long serialVersionUID = -5625119780401587251L;

}
