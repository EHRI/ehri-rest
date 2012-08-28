package eu.ehri.project.exceptions;

public class SerializationError extends Exception {

    /**
     * Serialization was attempted with input that did not match the required
     * specifications.
     */
    private static final long serialVersionUID = -1664595600301157596L;

    public SerializationError(String message) {
        super(message);
    }
}
