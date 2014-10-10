package eu.ehri.project.exceptions;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;

/**
 * Validation error. This exception holds a map of field=error(s) values.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public class ValidationError extends Exception {

    private static final long serialVersionUID = 1L;
    private final ErrorSet errorSet;
    private final Bundle bundle;

    public ValidationError(Bundle bundle, ErrorSet errorSet) {
        this.bundle = bundle;
        this.errorSet = errorSet;
    }

    public ValidationError(Bundle bundle, String key, String error) {        
        this(bundle, ErrorSet.fromError(key, error));
    }

    public ValidationError(Bundle bundle, ListMultimap<String, String> errors) {
        this(bundle, new ErrorSet(errors));
    }

    private static String formatErrors(String clsName, ErrorSet errorSet) {
        StringBuilder buf = new StringBuilder(String.format(
                "A validation error occurred building %s: %s\n", clsName, errorSet.toJson()));
        return buf.toString();
    }
    
    public String getMessage() {
    	return formatErrors(bundle.getType().getName(), errorSet);
    }

    public ErrorSet getErrorSet() {
        return errorSet;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
