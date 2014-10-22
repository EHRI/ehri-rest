package eu.ehri.project.exceptions;

/**
 * Represents the violation of an integrity constraint
 * during data persistence, typically due to
 * properties that must be unique across the entire
 * graph (e.g. identifiers.)
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
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
