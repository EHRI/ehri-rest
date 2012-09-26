package eu.ehri.project.exceptions;

/**
 * Base class for all EHRI Errors.
 * 
 * @author michaelb
 *
 */
public class EhriBaseError extends Exception {

    public EhriBaseError(String message) {
        super(message);
    }

    public EhriBaseError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 7644805801999880667L;

}
