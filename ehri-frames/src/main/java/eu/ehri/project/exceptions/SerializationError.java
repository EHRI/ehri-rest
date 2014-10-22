package eu.ehri.project.exceptions;

/**
 * Represents a failure to turn some internal data into an
 * external format.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SerializationError extends Exception {
    private static final long serialVersionUID = -1664595600301157596L;

    public SerializationError(String message) {
        super(message);
    }

    public SerializationError(String message, Throwable cause) {
        super(message, cause);
    }
}
