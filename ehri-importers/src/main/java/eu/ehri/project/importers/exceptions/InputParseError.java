package eu.ehri.project.importers.exceptions;

/**
 * The input data supplied was invalid in some way, detailed by @param cause.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputParseError extends Exception {
    private static final long serialVersionUID = -5295648478089620968L;

    /**
     * Constructor.
     * 
     * @param cause
     */
    public InputParseError(Throwable cause) {
        super(cause);
    }
}
