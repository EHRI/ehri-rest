package eu.ehri.project.exceptions;

public class DeserializationError extends Exception {

    /**
     * Deserialization was attempted with input that did not match the
     * required specifications.
     */
    private static final long serialVersionUID = -1664595600301157596L;

    public DeserializationError(String message) {
        super(message);
    }
}
