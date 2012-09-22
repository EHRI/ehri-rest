package eu.ehri.project.exceptions;

public class IndexNotFoundException extends Exception {

    /**
     * A query was attempted on an index that does not yet exist.
     */
    private static final long serialVersionUID = 1780204249581851165L;

    public IndexNotFoundException(String message) {
        super(message);
    }

    public IndexNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
