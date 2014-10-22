package eu.ehri.project.exceptions;

/**
 * Represents an error caused when attempting to deserialize
 * incoming data to some internal format.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DeserializationError extends Exception {

    /**
     * Deserialization was attempted with input that did not match the required
     * specifications.
     */
    private static final long serialVersionUID = -1664595600301157596L;

    public DeserializationError(String message) {
        super(message);
    }

    public DeserializationError(String message, Throwable cause) {
        super(message, cause);
    }
}
